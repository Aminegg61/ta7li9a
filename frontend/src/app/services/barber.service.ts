import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { BarberSearchDto } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class BarberService {
  // Jme3na l-URL hna bach y-khdem l-kolchi (Barber w Client)
  private baseUrl = 'https://ta7li9a-backend.onrender.com/api';

  constructor(private http: HttpClient) {}

  updateStatus(status: 'ACTIVE' | 'FULL' | 'OFFLINE'): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/barber/status?status=${status}`, {});
  }
  
  getCurrentStatus(): Observable<string> {
   return this.http.get(`${this.baseUrl}/barber/status`, { responseType: 'text' });
  }

  // 🔥 Hna t-riglat l-URL dyal s-Search!
  searchBarbers(query: string): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`${this.baseUrl}/barbers/search?q=${query}`);
  }

  // 🔥 Hna t-riglat l-URL dyal Add Barber!
  addBarber(barberId: number): Observable<string> {
    return this.http.post(`${this.baseUrl}/barbers/add-barber/${barberId}`, {}, { responseType: 'text' }) as Observable<string>;
  }

  getMyBarbers(): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`${this.baseUrl}/barbers/my-barbers`);
  }

  getMyFavorites(): Observable<BarberSearchDto[]> {
    return this.http.get<BarberSearchDto[]>(`${this.baseUrl}/barbers/my-favorites`);
  }

  removeBarber(barberId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/barbers/remove-barber/${barberId}`);
  }

  toggleFavorite(barberId: number): Observable<void> {
    return this.http.put<void>(`${this.baseUrl}/barbers/toggle-favorite/${barberId}`, {});
  }
  
  pauseWork(): Observable<any> {
    return this.http.post(`${this.baseUrl}/barber/pause`, {});
  }

  resumeWork(): Observable<any> {
    return this.http.post(`${this.baseUrl}/barber/resume`, {});
  }
}
