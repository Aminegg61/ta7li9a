import { Injectable } from '@angular/core';
import SockJS from 'sockjs-client';
import * as Stomp from 'stompjs';
import { Subject } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class WebsocketService {
  private stompClient: any;
  // Subject bach n-publiyiw l-akhbar l-ga3 l-components
  public statusUpdates = new Subject<any>();

  connect(barberId?: number) {
    // 1. Kanakhdou l-Liyen mn environment machi Hardcoded
    const socket = new SockJS(`${environment.apiUrl.replace('/api', '')}/ws-ta7li9a`); 
    this.stompClient = Stomp.over(socket);

    // 🔥 2. HADA HOWA L-FIX (HEARTBEAT):
    // K-n-goulou l-Stomp i-sifet rsala khawya (Ping) kol 60 taniya (60000ms)
    this.stompClient.heartbeat.outgoing = 60000; 
    // Ma-k-y-hmnach n-tsennaw Ping mn l-Backend
    this.stompClient.heartbeat.incoming = 0; 
    
    // T-7yid d-s-sda3 d-les logs f l-Console d-Production
    if (environment.production) {
      this.stompClient.debug = null; 
    }

    this.stompClient.connect({}, (frame: any) => {
      console.log('✅ Connected to WebSocket: ' + frame);

      // Subscribe l-dak l-topic khass b dak l-barber (ila kan mssajel)
      if (barberId) {
        this.stompClient.subscribe('/topic/status/' + barberId, (message: any) => {
          if (message.body) {
            this.statusUpdates.next(message.body); // Sift l-khbar l-ga3 li m-abonniyin
          }
        });
      }
    }, (error: any) => {
      // 3. Auto-Reconnect: Ila ta7 l-khet b sbab chi mochkil d-Connexion awla 4G
      console.error('❌ Erreur d-WebSocket, ghadi n-3awdou n-tconnectaw...', error);
      setTimeout(() => this.connect(barberId), 5000); // K-y-3awd idreq l-bab mor 5 tawanin
    });
  }

  // 🔥 L-FIX HNA: Qaddina l-Queue bach t-qra "UPDATE_QUEUE" bla ma t-planta (Try/Catch)
  subscribeToQueue(barberId: number) {
    const queueSubject = new Subject<any>();
    
    const trySubscribe = () => {
      if (this.stompClient && this.stompClient.connected) {
        this.stompClient.subscribe(`/topic/queue/${barberId}`, (message: any) => {
          try {
            queueSubject.next(JSON.parse(message.body));
          } catch (e) {
            queueSubject.next(message.body); // Ila kant String 3adiya ("UPDATE_QUEUE") ghadi t-douz
          }
        });
      } else {
        // Ila kan baqi ma t-connectach, n-tsennawh i-t-connecta w n-siftou
        setTimeout(trySubscribe, 1000);
      }
    };

    trySubscribe();
    return queueSubject.asObservable();
  }

  // 🔥 L-Fonction jdida li kiy-st3mlha l-ClientDashboard
  subscribeToUser(userId: number) {
    const userSubject = new Subject<any>();
    
    const trySubscribe = () => {
      if (this.stompClient && this.stompClient.connected) {
        // K-y-tsennet l-topic dyal l-Klyan
        this.stompClient.subscribe(`/topic/user/${userId}`, (message: any) => {
          try {
            userSubject.next(JSON.parse(message.body));
          } catch (e) {
            userSubject.next(message.body);
          }
        });
      } else {
        // Ila baqi ma-t-connectach, kiy-tsenna taniya w y-3awd y-jerreb
        setTimeout(trySubscribe, 1000);
      }
    };

    trySubscribe();
    return userSubject.asObservable();
  }

  disconnect() {
    if (this.stompClient) {
      this.stompClient.disconnect();
      console.log('🛑 WebSocket Disconnected');
    }
  }
}