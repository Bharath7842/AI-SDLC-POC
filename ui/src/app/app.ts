import { Component, signal } from '@angular/core';
import { PortRequestComponent } from './port-request.component';

@Component({
  selector: 'app-root',
  imports: [PortRequestComponent],
  template: `
    <app-port-request></app-port-request>
  `,
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('ui');
}
