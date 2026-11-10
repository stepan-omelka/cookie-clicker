import { ChangeDetectionStrategy, Component, ElementRef, inject, signal } from '@angular/core';
import { GameStore } from './game-store';

interface Pop {
  id: number;
  amount: number;
  x: number;
  y: number;
}

@Component({
  selector: 'app-cookie',
  templateUrl: './cookie.html',
  styleUrl: './cookie.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Cookie {
  private readonly store = inject(GameStore);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);
  private nextPopId = 0;

  protected readonly pops = signal<Pop[]>([]);

  protected bake(event: MouseEvent): void {
    const amount = this.store.click();
    if (amount === 0) {
      return;
    }
    const rect = this.host.nativeElement.getBoundingClientRect();
    const fromKeyboard = event.detail === 0;
    const x = fromKeyboard ? rect.width / 2 : event.clientX - rect.left;
    const y = fromKeyboard ? rect.height / 2 : event.clientY - rect.top;
    this.pops.update((pops) => [...pops, { id: this.nextPopId++, amount, x, y }]);
  }

  protected removePop(id: number): void {
    this.pops.update((pops) => pops.filter((pop) => pop.id !== id));
  }
}
