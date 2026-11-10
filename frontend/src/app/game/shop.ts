import { DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { GameStore } from './game-store';

const ICONS: Record<string, string> = {
  cursor: '👆',
  grandma: '👵',
  'rolling-pin': '🧑‍🍳',
  farm: '🌾',
  factory: '🏭',
};

@Component({
  selector: 'app-shop',
  imports: [DecimalPipe],
  templateUrl: './shop.html',
  styleUrl: './shop.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shop {
  protected readonly store = inject(GameStore);

  protected icon(upgradeId: string): string {
    return ICONS[upgradeId] ?? '🍪';
  }
}
