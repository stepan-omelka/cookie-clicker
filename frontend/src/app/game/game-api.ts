import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { AppTitle, Game } from './game.models';

@Injectable({ providedIn: 'root' })
export class GameApi {
  private readonly http = inject(HttpClient);

  getTitle() {
    return this.http.get<AppTitle>('/api/title');
  }

  getGame() {
    return this.http.get<Game>('/api/game');
  }

  click(count: number) {
    return this.http.post<Game>('/api/game/clicks', { count });
  }

  buy(upgradeId: string) {
    return this.http.post<Game>(`/api/game/upgrades/${encodeURIComponent(upgradeId)}`, null);
  }

  reset() {
    return this.http.delete<void>('/api/game');
  }

  sendClicksOnExit(count: number) {
    const body = new Blob([JSON.stringify({ count })], { type: 'application/json' });
    return navigator.sendBeacon('/api/game/clicks', body);
  }
}
