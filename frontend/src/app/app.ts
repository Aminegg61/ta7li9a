import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';

// 👇 Hada s-ster l-lowel li zedna (kayjib l-Footer)
import { FooterComponent } from './components/footer/footer.component';

@Component({
  selector: 'app-root',
  // 👇 Hna dkhlna FooterComponent m3a RouterOutlet
  imports: [RouterOutlet, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('ta7li9a-front');
}
