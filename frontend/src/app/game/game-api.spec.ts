import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { GameApi } from './game-api';

describe('GameApi', () => {
  let api: GameApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(GameApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends clicks as a JSON body', () => {
    api.click(7).subscribe();

    const request = http.expectOne('/api/game/clicks');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ count: 7 });
  });

  it('buys an upgrade by id', () => {
    api.buy('rolling-pin').subscribe();

    expect(http.expectOne('/api/game/upgrades/rolling-pin').request.method).toBe('POST');
  });

  it('resets the game with DELETE', () => {
    api.reset().subscribe();

    expect(http.expectOne('/api/game').request.method).toBe('DELETE');
  });
});
