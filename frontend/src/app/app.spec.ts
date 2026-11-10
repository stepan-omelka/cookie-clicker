import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { App } from './app';
import { aGame } from './testing/game-fixture';

describe('App', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('renders the title and the game from the API', async () => {
    const fixture = TestBed.createComponent(App);
    http.expectOne('/api/title').flush({ id: 1, text: 'Grandma Bakery' });
    http.expectOne('/api/game').flush(aGame({ cookies: 1234 }));
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Grandma Bakery');
    expect(element.querySelector('.number')?.textContent).toContain('1,234');
    expect(element.querySelectorAll('.upgrade').length).toBe(1);
  });

  it('offers a retry when the backend is down', async () => {
    const fixture = TestBed.createComponent(App);
    http.expectOne('/api/title').error(new ProgressEvent('error'));
    http.expectOne('/api/game').error(new ProgressEvent('error'));
    await fixture.whenStable();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('h1')?.textContent).toContain('Cookie Clicker');
    expect(element.textContent).toContain('The bakery is closed');
  });
});
