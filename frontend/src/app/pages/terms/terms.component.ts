import { Component } from '@angular/core';

@Component({
  selector: 'app-terms',
  standalone: true,
  template: `
    <div class="container mx-auto px-4 py-12 text-gray-300 max-w-4xl">
      <h1 class="text-3xl font-bold text-white mb-6">Terms of Service</h1>
      <p class="mb-4">Last updated: May 2026</p>
      
      <div class="space-y-6">
        <section>
          <h2 class="text-xl font-semibold text-white mb-2">1. Agreement to Terms</h2>
          <p>By accessing or using T7li9a, you agree to be bound by these Terms of Service. If you disagree with any part of the terms, you may not access our platform.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">2. Description of Service</h2>
          <p>T7li9a is a digital platform designed to connect barbers with their clients, facilitating seamless appointment booking and time management. We provide the software infrastructure but are not responsible for the actual grooming services provided by the barbers.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">3. WhatsApp Communications</h2>
          <p>By registering on T7li9a and providing your phone number, you explicitly consent to receive transactional messages, appointment reminders, and platform updates via WhatsApp. You can opt out of these communications at any time by contacting support or replying "STOP" to our WhatsApp number.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">4. User Responsibilities</h2>
          <p>You agree to provide accurate, current, and complete information during the registration and booking process. You are responsible for maintaining the confidentiality of your account and for all activities that occur under your account.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">5. Cancellations and Modifications</h2>
          <p>Appointment cancellations or modifications should be made according to the specific barber's policy displayed on our platform. T7li9a reserves the right to suspend accounts that frequently miss appointments without prior notice.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">6. Governing Law</h2>
          <p>These terms and conditions are governed by and construed in accordance with the laws of the Kingdom of Morocco.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">7. Contact Information</h2>
          <p>For any inquiries regarding these Terms, please contact us at:</p>
          <p class="mt-2 text-yellow-500">Email: contact@[YOUR-DOMAIN].ma</p>
        </section>
      </div>
    </div>
  `
})
export class TermsComponent {}