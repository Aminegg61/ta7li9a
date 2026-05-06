import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AppointmentResponseDTO, AppointmentRequestDTO, User } from '../models/interfaces';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private baseUrl = 'https://ta7li9a-backend.onrender.com/api/appointments';

  constructor(private http: HttpClient) {}

  searchClients(query: string): Observable<User[]> {
    return this.http.get<User[]>(`${this.baseUrl}/search-clients?query=${query}`);
  }

  createAppointment(dto: AppointmentRequestDTO): Observable<AppointmentResponseDTO> {
    return this.http.post<AppointmentResponseDTO>(`${this.baseUrl}/add`, dto);
  }

  getTodayQueue(): Observable<AppointmentResponseDTO[]> {
    return this.http.get<AppointmentResponseDTO[]>(`${this.baseUrl}/today-queue`);
  }

  startAppointment(id: number): Observable<AppointmentResponseDTO> {
    return this.http.put<AppointmentResponseDTO>(`${this.baseUrl}/${id}/start`, {});
  }

  completeAppointment(id: number): Observable<AppointmentResponseDTO> {
    return this.http.put<AppointmentResponseDTO>(`${this.baseUrl}/${id}/done`, {});
  }
  acceptAppointment(id: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}/accept`, {});
  }

  rejectAppointment(id: number): Observable<any> {
    return this.http.put(`${this.baseUrl}/${id}/reject`, {});
  }
  getMyActiveAppointment(): Observable<AppointmentResponseDTO[]> {
    return this.http.get<AppointmentResponseDTO[]>(`${this.baseUrl}/my-active`);
  }
  clearAppointment(id: number) {
    return this.http.put(`${this.baseUrl}/${id}/clear`, {});
  }
  // 🔥 Start service wahed
  startItem(itemId: number) {
    return this.http.put(`${this.baseUrl}/items/${itemId}/start`, {});
  }

  // 🔥 Sali service wahed
  completeItem(itemId: number) {
    return this.http.put(`${this.baseUrl}/items/${itemId}/complete`, {});
  }
  getMyRequests(): Observable<AppointmentResponseDTO[]> {
    return this.http.get<AppointmentResponseDTO[]>(`${this.baseUrl}/client/my-requests`);
  }
}
