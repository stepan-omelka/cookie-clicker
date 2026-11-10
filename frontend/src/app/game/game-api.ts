import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AppTitle, ClickBatch, Game } from './game.models';

@Injectable({ providedIn: 'root' })
export class GameApi {
  private readonly http = inject(HttpClient);

  getTitle() {
    return this.http.get<AppTitle>('/api/title');
  }

  getGame() {
    return this.http.get<Game>('/api/game');
  }

  click(batch: ClickBatch) {
    return this.http.post<Game>('/api/game/clicks', batch, { keepalive: true });
  }

  buy(upgradeId: string) {
    return this.http.post<Game>(`/api/game/upgrades/${encodeURIComponent(upgradeId)}`, null);
  }

  reset() {
    return this.http.delete<Game>('/api/game');
  }

  sendClicksOnExit(batch: ClickBatch): boolean {
    const body = new Blob([JSON.stringify(batch)], { type: 'application/json' });
    return navigator.sendBeacon('/api/game/clicks', body);
  }
}
