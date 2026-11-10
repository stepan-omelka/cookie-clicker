import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { aGame } from '../testing/game-fixture';
import { GameApi } from './game-api';
import { GameStore } from './game-store';

describe('GameStore', () => {
  let store: GameStore;
  let http: HttpTestingController;

  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(GameStore);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    vi.useRealTimers();
  });

  function loadGame(game = aGame()) {
    store.load();
    http.expectOne('/api/game').flush(game);
  }

  function clickTimes(times: number) {
    for (let i = 0; i < times; i++) {
      store.click();
    }
  }

  it('becomes ready after loading the game', () => {
    expect(store.status()).toBe('loading');

    loadGame(aGame({ cookies: 42 }));

    expect(store.status()).toBe('ready');
    expect(store.cookies()).toBe(42);
  });

  it('goes offline when the first load fails', () => {
    store.load();
    http.expectOne('/api/game').error(new ProgressEvent('error'));

    expect(store.status()).toBe('offline');
  });

  it('shows clicks immediately and sends them in one batch', () => {
    loadGame(aGame({ perClick: 2 }));

    clickTimes(3);
    expect(store.cookies()).toBe(6);
    http.expectNone('/api/game/clicks');

    vi.advanceTimersByTime(1000);
    const request = http.expectOne('/api/game/clicks');
    expect(request.request.body).toEqual({ id: expect.any(String), count: 3 });

    request.flush(aGame({ cookies: 6, perClick: 2, totalClicks: 3 }));
    expect(store.cookies()).toBe(6);
    expect(store.totalClicks()).toBe(3);
  });

  it('keeps clicks made while a batch is in flight', () => {
    loadGame();
    store.click();
    vi.advanceTimersByTime(1000);
    const request = http.expectOne('/api/game/clicks');

    store.click();
    request.flush(aGame({ cookies: 1, totalClicks: 1 }));

    expect(store.cookies()).toBe(2);
  });

  it('retries a failed batch with the same id', () => {
    loadGame();
    clickTimes(4);

    vi.advanceTimersByTime(1000);
    const first = http.expectOne('/api/game/clicks');
    first.error(new ProgressEvent('error'));
    expect(store.notice()).toContain("Can't reach the bakery");
    expect(store.cookies()).toBe(4);

    store.click();
    vi.advanceTimersByTime(1000);
    const retry = http.expectOne('/api/game/clicks');
    expect(retry.request.body).toEqual(first.request.body);

    retry.flush(aGame({ cookies: 4, totalClicks: 4 }));
    expect(store.notice()).toBeNull();
    expect(store.cookies()).toBe(5);
  });

  it('drops a batch the server rejects as invalid', () => {
    loadGame();
    clickTimes(2);

    vi.advanceTimersByTime(1000);
    http
      .expectOne('/api/game/clicks')
      .flush({ detail: 'Invalid' }, { status: 400, statusText: 'Bad Request' });

    expect(store.cookies()).toBe(0);
    vi.advanceTimersByTime(1000);
    http.expectNone('/api/game/clicks');
  });

  it('gives up on a request that hangs', () => {
    loadGame();
    store.click();

    vi.advanceTimersByTime(1000);
    const hanging = http.expectOne('/api/game/clicks');
    vi.advanceTimersByTime(10_000);

    expect(hanging.cancelled).toBe(true);
    expect(store.notice()).toContain("Can't reach the bakery");
    http.expectOne('/api/game/clicks');
  });

  it('adds passive income between syncs', () => {
    loadGame(aGame({ cookies: 10, perSecond: 2 }));

    vi.advanceTimersByTime(1500);

    expect(store.cookies()).toBe(13);
  });

  it('sends pending clicks before buying', () => {
    loadGame();
    clickTimes(15);

    store.buy('cursor');

    const clicks = http.expectOne('/api/game/clicks');
    expect(clicks.request.body).toEqual({ id: expect.any(String), count: 15 });
    http.expectNone('/api/game/upgrades/cursor');
    clicks.flush(aGame({ cookies: 15, totalClicks: 15 }));

    http.expectOne('/api/game/upgrades/cursor').flush(aGame({ cookies: 0, perClick: 2 }));
    expect(store.perClick()).toBe(2);
  });

  it('warns when the server rejects a purchase', () => {
    loadGame();

    store.buy('cursor');
    http
      .expectOne('/api/game/upgrades/cursor')
      .flush({ detail: 'Not enough' }, { status: 409, statusText: 'Conflict' });

    expect(store.notice()).toBe('Not enough cookies yet.');
    vi.advanceTimersByTime(3000);
    expect(store.notice()).toBeNull();
  });

  it('starts over on reset and forgets unsent clicks', () => {
    loadGame(aGame({ cookies: 500 }));
    clickTimes(3);

    store.reset();
    http.expectOne({ method: 'DELETE', url: '/api/game' }).flush(aGame());

    expect(store.cookies()).toBe(0);
    vi.advanceTimersByTime(1000);
    http.expectNone('/api/game/clicks');
  });

  it('sends unsent clicks with a beacon when the page closes', () => {
    const sendBeacon = vi.spyOn(TestBed.inject(GameApi), 'sendClicksOnExit').mockReturnValue(true);
    loadGame();
    clickTimes(3);

    window.dispatchEvent(new Event('pagehide'));

    expect(sendBeacon).toHaveBeenCalledWith({ id: expect.any(String), count: 3 });
    vi.advanceTimersByTime(1000);
    http.expectNone('/api/game/clicks');
  });
});
