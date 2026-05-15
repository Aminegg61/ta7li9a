import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BarberSearchDto } from '../models/interfaces';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class BarberService {
  private baseUrl = `${environment.apiUrl}/api/barber`;

  constructor(private http: HttpClient) {}

  updateStatus(status: 'ACTIVE' | 'FULL' | 'OFFLINE'): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/status?status=${status}`, {});
  }
  getCurrentStatus(): Observable<any> {
   return this.http.get(`${this.baseUrl}/status`);
  }

  searchBarbers(query: string): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`${environment.apiUrl}/api/barbers/search?q=${query}`);
  }

  addBarber(barberId: number): Observable<string> {
    // Expect text response or format appropriately based on your backend
    return this.http.post(`${environment.apiUrl}/api/barbers/add-barber/` + barberId, {}, { responseType: 'text' }) as Observable<string>;
  }

  getMyBarbers(): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`${environment.apiUrl}/api/barbers/my-barbers`);
  }

  getMyFavorites(): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`${environment.apiUrl}/api/barbers/my-favorites`);
  }

  removeBarber(barberId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/barbers/remove-barber/${barberId}`);
  }

  toggleFavorite(barberId: number): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/api/barbers/toggle-favorite/${barberId}`, {});
  }
  pauseWork(): Observable<any> {
    // T2ekked mn l-URL dyalk wach howa hada (masalan: baseUrl + '/barbers/pause')
    return this.http.post(`${this.baseUrl}/pause`, {});
  }

  resumeWork(): Observable<any> {
    return this.http.post(`${this.baseUrl}/resume`, {});
  }
  // 🔥 Zid hadi bach l-klyan y-jib weqto 3nd l-barber
  getMyCustomTimesForBarber(barberId: number): Observable<any[]> {
    return this.http.get<any[]>(`${environment.apiUrl}/api/barbers/my-custom-times/${barberId}`);
  }
}
