import { HttpErrorResponse } from '@angular/common/http';
import { computed, inject, Injectable, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  catchError,
  concatMap,
  EMPTY,
  finalize,
  fromEvent,
  interval,
  Observable,
  Subject,
  switchMap,
  tap,
} from 'rxjs';
import { GameApi } from './game-api';
import { Game } from './game.models';

export const MAX_CLICKS_PER_REQUEST = 500;
const TICK_MS = 100;
const FLUSH_MS = 1000;
const REFRESH_MS = 30_000;
const NOTICE_MS = 3000;
const OFFLINE_NOTICE = "Can't reach the bakery. Your clicks are safe, retrying…";

type Status = 'loading' | 'ready' | 'offline';

interface Command {
  request: () => Observable<Game>;
  onSuccess?: () => void;
  onError?: (error: unknown) => void;
  onFinish?: () => void;
}

@Injectable({ providedIn: 'root' })
export class GameStore {
  private readonly api = inject(GameApi);
  private readonly commands = new Subject<Command>();

  private readonly serverGame = signal<Game | null>(null);
  private readonly syncedAt = signal(Date.now());
  private readonly now = signal(Date.now());
  private readonly unsyncedClicks = signal(0);
  private queuedClicks = 0;

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
          command.request().pipe(
            tap((game) => {
              command.onSuccess?.();
              this.apply(game);
            }),
            catchError((error) => {
              command.onError?.(error);
              this.handleError(error);
              return EMPTY;
            }),
            finalize(() => command.onFinish?.()),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe();

    interval(TICK_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.now.set(Date.now()));
    interval(FLUSH_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.flushClicks());
    interval(REFRESH_MS)
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.refresh());

    if (typeof window !== 'undefined') {
      fromEvent(window, 'pagehide')
        .pipe(takeUntilDestroyed())
        .subscribe(() => this.sendClicksOnExit());
    }
  }

  load(): void {
    this.status.set('loading');
    this.enqueue({ request: () => this.api.getGame() });
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
    this.flushClicks();
    this.enqueue({
      request: () => this.api.buy(upgradeId),
      onError: (error) => {
        if (error instanceof HttpErrorResponse && error.status === 409) {
          this.flash('Not enough cookies yet.');
        }
      },
    });
  }

  reset(): void {
    this.flushClicks();
    this.enqueue({ request: () => this.api.reset().pipe(switchMap(() => this.api.getGame())) });
  }

  private refresh(): void {
    if (this.status() === 'ready') {
      this.enqueue({ request: () => this.api.getGame() });
    }
  }

  private flushClicks(): void {
    let remaining = this.unsyncedClicks() - this.queuedClicks;
    while (remaining > 0) {
      const count = Math.min(remaining, MAX_CLICKS_PER_REQUEST);
      remaining -= count;
      this.queuedClicks += count;
      this.enqueue({
        request: () => this.api.click(count),
        onSuccess: () => this.unsyncedClicks.update((clicks) => Math.max(0, clicks - count)),
        onFinish: () => (this.queuedClicks -= count),
      });
    }
  }

  private sendClicksOnExit(): void {
    let remaining = this.unsyncedClicks() - this.queuedClicks;
    while (remaining > 0) {
      const count = Math.min(remaining, MAX_CLICKS_PER_REQUEST);
      if (!this.api.sendClicksOnExit(count)) {
        return;
      }
      remaining -= count;
      this.unsyncedClicks.update((clicks) => clicks - count);
    }
  }

  private enqueue(command: Command): void {
    this.commands.next(command);
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
      return;
    }
    if (error instanceof HttpErrorResponse && error.status === 409) {
      return;
    }
    this.notice.set(OFFLINE_NOTICE);
  }
}
