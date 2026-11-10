import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  concatMap,
  defer,
  EMPTY,
  filter,
  fromEvent,
  interval,
  Observable,
  Subject,
  tap,
  throwError,
  timeout,
} from 'rxjs';
import { GameApi } from './game-api';
import { ClickBatch, Game } from './game.models';

export const MAX_CLICKS_PER_REQUEST = 500;
const TICK_MS = 100;
const FLUSH_MS = 1000;
const REFRESH_MS = 30_000;
const REQUEST_TIMEOUT_MS = 10_000;
const NOTICE_MS = 3000;
const OFFLINE_NOTICE = "Can't reach the bakery. Your clicks are safe, retrying…";

type Status = 'loading' | 'ready' | 'offline';

@Injectable({ providedIn: 'root' })
export class GameStore {
  private readonly api = inject(GameApi);
  private readonly commands = new Subject<() => Observable<Game>>();

  private readonly serverGame = signal<Game | null>(null);
  private readonly syncedAt = signal(Date.now());
  private readonly now = signal(Date.now());
  private readonly unsyncedClicks = signal(0);
  private batch: ClickBatch | null = null;
  private queuedFlushes = 0;

  readonly status = signal<Status>('loading');
  readonly notice = signal<string | null>(null);

  readonly perClick = computed(() => this.serverGame()?.perClick ?? 1);
  readonly perSecond = computed(() => this.serverGame()?.perSecond ?? 0);
  readonly upgrades = computed(() => this.serverGame()?.upgrades ?? []);
  readonly totalClicks = computed(
    () => (this.serverGame()?.totalClicks ?? 0) + this.unsyncedClicks(),
  );
  readonly cookies = computed(() => {
    const game = this.serverGame();
    if (!game) {
      return 0;
    }
    const elapsedSeconds = Math.max(0, this.now() - this.syncedAt()) / 1000;
    const passive = game.perSecond * elapsedSeconds;
    return Math.floor(game.cookies + passive + this.unsyncedClicks() * game.perClick);
  });

  constructor() {
    this.commands
      .pipe(
        concatMap((command) =>
          defer(command).pipe(
            timeout(REQUEST_TIMEOUT_MS),
            tap((game) => this.apply(game)),
            catchError((error) => {
              this.handleError(error);
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe();

    interval(TICK_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(Date.now()));
    interval(FLUSH_MS)
      .pipe(
        filter(() => this.queuedFlushes === 0),
        takeUntilDestroyed(),
      )
      .subscribe(() => this.flushClicks());
    interval(REFRESH_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.refresh());

    if (typeof window !== 'undefined') {
      fromEvent(window, 'pagehide')
        .pipe(takeUntilDestroyed())
        .subscribe(() => this.sendClicksOnExit());
      fromEvent<PageTransitionEvent>(window, 'pageshow')
        .pipe(
          filter((event) => event.persisted),
          takeUntilDestroyed(),
        )
        .subscribe(() => this.refresh());
    }
  }

  load(): void {
    this.status.set('loading');
    this.commands.next(() => this.api.getGame());
  }

  click(): number {
    if (this.status() !== 'ready') {
      return 0;
    }
    this.unsyncedClicks.update((count) => count + 1);
    return this.perClick();
  }

  canAfford(cost: number): boolean {
    return this.cookies() >= cost;
  }

  buy(upgradeId: string): void {
    this.flushClicks(true);
    this.commands.next(() =>
      this.api.buy(upgradeId).pipe(
        catchError((error) => {
          if (error instanceof HttpErrorResponse && error.status === 409) {
            this.flash('Not enough cookies yet.');
            return EMPTY;
          }
          return throwError(() => error);
        }),
      ),
    );
  }

  reset(): void {
    this.commands.next(() => {
      this.batch = null;
      this.unsyncedClicks.set(0);
      return this.api.reset();
    });
  }

  private refresh(): void {
    if (this.status() === 'ready') {
      this.commands.next(() => this.api.getGame());
    }
  }

  private unsentClicks(): number {
    return this.unsyncedClicks() - (this.batch?.count ?? 0);
  }

  private flushClicks(all = false): void {
    const pending = (this.batch ? 1 : 0) + Math.ceil(this.unsentClicks() / MAX_CLICKS_PER_REQUEST);
    const flushes = all ? pending : Math.min(pending, 1);
    for (let i = 0; i < flushes; i++) {
      this.queuedFlushes++;
      this.commands.next(() => {
        this.queuedFlushes--;
        return this.sendClicks();
      });
    }
  }

  // the batch size is decided when the request goes out, retries reuse the same id
  private sendClicks(): Observable<Game> {
    if (!this.batch) {
      const count = Math.min(this.unsentClicks(), MAX_CLICKS_PER_REQUEST);
      if (count <= 0) {
        return EMPTY;
      }
      this.batch = { id: crypto.randomUUID(), count };
    }
    const batch = this.batch;
    return this.api.click(batch).pipe(
      tap(() => this.settle(batch)),
      catchError((error) => {
        if (!isTransient(error)) {
          this.settle(batch);
        }
        return throwError(() => error);
      }),
    );
  }

  private settle(batch: ClickBatch): void {
    if (this.batch === batch) {
      this.batch = null;
      this.unsyncedClicks.update((clicks) => Math.max(0, clicks - batch.count));
    }
  }

  private sendClicksOnExit(): void {
    const batches = this.batch ? [this.batch] : [];
    for (let unsent = this.unsentClicks(); unsent > 0; unsent -= MAX_CLICKS_PER_REQUEST) {
      batches.push({ id: crypto.randomUUID(), count: Math.min(unsent, MAX_CLICKS_PER_REQUEST) });
    }
    for (const batch of batches) {
      if (!this.api.sendClicksOnExit(batch)) {
        return;
      }
      if (this.batch === batch) {
        this.batch = null;
      }
      this.unsyncedClicks.update((clicks) => Math.max(0, clicks - batch.count));
    }
  }

  private apply(game: Game): void {
    const now = Date.now();
    this.serverGame.set(game);
    this.syncedAt.set(now);
    this.now.set(now);
    this.status.set('ready');
    if (this.notice() === OFFLINE_NOTICE) {
      this.notice.set(null);
    }
  }

  private flash(message: string): void {
    this.notice.set(message);
    setTimeout(() => {
      if (this.notice() === message) {
        this.notice.set(null);
      }
    }, NOTICE_MS);
  }

  private handleError(error: unknown): void {
    if (this.status() === 'loading') {
      this.status.set('offline');
    } else if (isTransient(error)) {
      this.notice.set(OFFLINE_NOTICE);
    } else {
      this.flash('Something went wrong. Please try again.');
    }
  }
}

function isTransient(error: unknown): boolean {
  return !(error instanceof HttpErrorResponse) || error.status === 0 || error.status >= 500;
}
