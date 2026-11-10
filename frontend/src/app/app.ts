import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { Title } from '@angular/platform-browser';
import { catchError, map, of } from 'rxjs';
import { Cookie } from './game/cookie';
import { GameApi } from './game/game-api';
import { GameStore } from './game/game-store';
import { Shop } from './game/shop';

const FALLBACK_TITLE = 'Cookie Clicker';
const RESET_CONFIRM_MS = 3000;

@Component({
  selector: 'app-root',
  imports: [Cookie, Shop, DecimalPipe],
  templateUrl: './app.html',
  styleUrl: './app.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class App {
  private readonly api = inject(GameApi);
  private readonly documentTitle = inject(Title);
  private resetTimer?: ReturnType<typeof setTimeout>;

  protected readonly store = inject(GameStore);
  protected readonly confirmingReset = signal(false);
  protected readonly title = toSignal(
    this.api.getTitle().pipe(
      map((title) => title.text),
      catchError(() => of(FALLBACK_TITLE)),
    ),
    { initialValue: FALLBACK_TITLE },
  );

  constructor() {
    this.store.load();
    effect(() => this.documentTitle.setTitle(this.title()));
  }

  protected reset(): void {
    clearTimeout(this.resetTimer);
    if (!this.confirmingReset()) {
      this.confirmingReset.set(true);
      this.resetTimer = setTimeout(() => this.confirmingReset.set(false), RESET_CONFIRM_MS);
      return;
    }
    this.confirmingReset.set(false);
    this.store.reset();
  }
}
