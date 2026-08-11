import { Component, OnInit, signal } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

interface PortRequest {
  id: string;
  status: string;
  createdAt: string;
  completedAt: string | null;
}

@Component({
  selector: 'app-port-request',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="container">
      <h1>Port Request Portal</h1>

      <div class="form-section">
        <h2>Submit New Request</h2>
        <div class="form-group">
          <label for="customerId">Customer ID:</label>
          <input
            id="customerId"
            [(ngModel)]="customerId"
            placeholder="Enter customer ID"
            (keyup.enter)="submitRequest()"
          />
        </div>
        <button
          (click)="submitRequest()"
          [disabled]="!customerId || isLoading()"
        >
          {{ isLoading() ? 'Submitting...' : 'Submit Request' }}
        </button>

        <div *ngIf="error()" class="error-message">
          {{ error() }}
        </div>
      </div>

      <div *ngIf="requestId()" class="status-section">
        <h2>Request Status</h2>
        <div class="status-info">
          <p><strong>Request ID:</strong> {{ requestId() }}</p>
          <p><strong>Status:</strong> <span class="status" [ngClass]="currentRequest()?.status">{{ currentRequest()?.status }}</span></p>
          <p *ngIf="currentRequest()"><strong>Created:</strong> {{ currentRequest()?.createdAt | date:'medium' }}</p>
          <p *ngIf="currentRequest()?.completedAt"><strong>Completed:</strong> {{ currentRequest()?.completedAt | date:'medium' }}</p>
        </div>
        <button (click)="refreshStatus()" [disabled]="isLoading()">
          {{ isLoading() ? 'Refreshing...' : 'Refresh Status' }}
        </button>
        <button (click)="clearStatus()" class="secondary">
          Clear
        </button>
      </div>

      <div class="info-section">
        <h3>API Configuration</h3>
        <p>API URL: {{ apiUrl }}</p>
        <p>API Key: {{ apiKey || 'Not configured (set in environment)' }}</p>
      </div>
    </div>
  `,
  styles: [`
    .container {
      max-width: 600px;
      margin: 2rem auto;
      padding: 2rem;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    }

    h1 {
      color: #333;
      text-align: center;
    }

    .form-section, .status-section, .info-section {
      margin: 2rem 0;
      padding: 1.5rem;
      background: #f9f9f9;
      border-radius: 8px;
      border: 1px solid #e0e0e0;
    }

    .form-group {
      margin: 1rem 0;
    }

    label {
      display: block;
      margin-bottom: 0.5rem;
      font-weight: 500;
      color: #333;
    }

    input {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
      box-sizing: border-box;
    }

    input:focus {
      outline: none;
      border-color: #0066cc;
      box-shadow: 0 0 0 3px rgba(0, 102, 204, 0.1);
    }

    button {
      padding: 0.75rem 1.5rem;
      background: #0066cc;
      color: white;
      border: none;
      border-radius: 4px;
      font-size: 1rem;
      cursor: pointer;
      margin-right: 0.5rem;
      margin-top: 1rem;
    }

    button:hover:not(:disabled) {
      background: #0052a3;
    }

    button:disabled {
      background: #ccc;
      cursor: not-allowed;
    }

    button.secondary {
      background: #666;
    }

    button.secondary:hover {
      background: #555;
    }

    .error-message {
      margin-top: 1rem;
      padding: 1rem;
      background: #fee;
      border-left: 4px solid #f00;
      color: #c00;
      border-radius: 4px;
    }

    .status-info {
      margin: 1rem 0;
    }

    .status-info p {
      margin: 0.5rem 0;
      font-size: 0.95rem;
    }

    .status {
      padding: 0.25rem 0.75rem;
      border-radius: 4px;
      font-weight: 600;
    }

    .status.INITIATED {
      background: #e3f2fd;
      color: #1976d2;
    }

    .status.COMPLETED {
      background: #e8f5e9;
      color: #388e3c;
    }

    .info-section {
      background: #f0f0f0;
      font-size: 0.9rem;
      color: #666;
    }

    .info-section p {
      margin: 0.25rem 0;
      word-break: break-all;
    }
  `]
})
export class PortRequestComponent implements OnInit {
  customerId = '';
  requestId = signal<string | null>(null);
  currentRequest = signal<PortRequest | null>(null);
  isLoading = signal(false);
  error = signal<string | null>(null);

  apiUrl: string = '';
  apiKey: string = '';

  constructor(private http: HttpClient) {}

  ngOnInit() {
    // Get API URL from environment or use localhost default
    const apiUrl = (window as any).__env?.apiUrl || 'http://localhost:8080';
    this.apiUrl = apiUrl;
    this.apiKey = localStorage.getItem('apiKey') || 'test-key-local';
  }

  submitRequest() {
    if (!this.customerId.trim()) {
      this.error.set('Please enter a customer ID');
      return;
    }

    this.isLoading.set(true);
    this.error.set(null);

    const headers = this.getHeaders();
    const body = { customerId: this.customerId };

    this.http.post<PortRequest>(`${this.apiUrl}/api/v1/port-requests`, body, { headers })
      .subscribe({
        next: (response) => {
          this.requestId.set(response.id);
          this.currentRequest.set(response);
          this.customerId = '';
          this.isLoading.set(false);
        },
        error: (err) => {
          this.error.set(`Error: ${err.error?.error || err.statusText || 'Unknown error'}`);
          this.isLoading.set(false);
        }
      });
  }

  refreshStatus() {
    const id = this.requestId();
    if (!id) return;

    this.isLoading.set(true);
    this.error.set(null);

    const headers = this.getHeaders();
    this.http.get<PortRequest>(`${this.apiUrl}/api/v1/port-requests/${id}`, { headers })
      .subscribe({
        next: (response) => {
          this.currentRequest.set(response);
          this.isLoading.set(false);
        },
        error: (err) => {
          this.error.set(`Error: ${err.error?.error || err.statusText || 'Unknown error'}`);
          this.isLoading.set(false);
        }
      });
  }

  clearStatus() {
    this.requestId.set(null);
    this.currentRequest.set(null);
    this.error.set(null);
  }

  private getHeaders(): HttpHeaders {
    return new HttpHeaders({
      'Content-Type': 'application/json',
      'X-API-Key': this.apiKey
    });
  }
}
