import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { AuthService } from './auth.service';
import { ExportNotificationEvent } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class ExportWebsocketService implements OnDestroy {
  private socket: WebSocket | null = null;
  private readonly notificationSubject = new Subject<ExportNotificationEvent>();
  private reconnectTimer: any = null;
  private manuallyClosed = false;

  readonly notifications$: Observable<ExportNotificationEvent> =
    this.notificationSubject.asObservable();

  constructor(
    private authService: AuthService,
    private ngZone: NgZone
  ) {}

  connect(): void {
    const token = this.authService.getToken();
    if (!token) {
      return;
    }

    if (
      this.socket &&
      (this.socket.readyState === WebSocket.OPEN ||
        this.socket.readyState === WebSocket.CONNECTING)
    ) {
      return;
    }

    this.manuallyClosed = false;
    const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    // Kết nối tới Backend WebSocket (qua cổng 8080 hoặc proxy)
    const wsHost =
      window.location.port === '4200' ? `${window.location.hostname}:8080` : window.location.host;
    const wsUrl = `${wsProtocol}//${wsHost}/ws/export?token=${encodeURIComponent(token)}`;

    try {
      this.socket = new WebSocket(wsUrl);

      this.socket.onmessage = (event: MessageEvent) => {
        this.ngZone.run(() => {
          try {
            const payload = JSON.parse(event.data) as ExportNotificationEvent;
            this.notificationSubject.next(payload);
          } catch (err) {
            console.error('Lỗi parse WebSocket message:', err);
          }
        });
      };

      this.socket.onclose = () => {
        this.socket = null;
        if (!this.manuallyClosed && this.authService.isLoggedIn()) {
          this.scheduleReconnect();
        }
      };

      this.socket.onerror = () => {
        // Sẽ tự động gọi onclose và reconnect
      };
    } catch (e) {
      console.error('Không thể khởi tạo WebSocket:', e);
      this.scheduleReconnect();
    }
  }

  disconnect(): void {
    this.manuallyClosed = true;
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    if (this.socket) {
      this.socket.close();
      this.socket = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.reconnectTimer) {
      return;
    }
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null;
      if (!this.manuallyClosed && this.authService.isLoggedIn()) {
        this.connect();
      }
    }, 3000);
  }

  ngOnDestroy(): void {
    this.disconnect();
  }
}

