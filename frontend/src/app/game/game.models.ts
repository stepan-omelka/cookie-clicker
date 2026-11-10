export interface AppTitle {
  id: number;
  text: string;
}

export interface Upgrade {
  id: string;
  name: string;
  description: string;
  cost: number;
  owned: number;
  perClick: number;
  perSecond: number;
}

export interface Game {
  cookies: number;
  perClick: number;
  perSecond: number;
  totalClicks: number;
  upgrades: Upgrade[];
}
