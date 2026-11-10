import { Game } from '../game/game.models';

export function aGame(overrides: Partial<Game> = {}): Game {
  return {
    cookies: 0,
    perClick: 1,
    perSecond: 0,
    totalClicks: 0,
    upgrades: [
      {
        id: 'cursor',
        name: 'Cursor',
        description: '+1 cookie per click',
        cost: 15,
        owned: 0,
        perClick: 1,
        perSecond: 0,
      },
    ],
    ...overrides,
  };
}
