import { Component } from '@angular/core';

@Component({
  selector: 'app-privacy-policy',
  standalone: true,
  template: `
    <div class="container mx-auto px-4 py-12 text-gray-300 max-w-4xl">
      <h1 class="text-3xl font-bold text-white mb-6">Privacy Policy</h1>
      <p class="mb-4">Last updated: May 2026</p>
      
      <div class="space-y-6">
        <section>
          <h2 class="text-xl font-semibold text-white mb-2">1. Introduction</h2>
          <p>Welcome to T7li9a. We respect your privacy and are committed to protecting your personal data. This Privacy Policy explains how we collect, use, and safeguard your information when you use our platform to book barber appointments and receive updates via WhatsApp.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">2. Information We Collect</h2>
          <p>To facilitate appointment bookings between clients and barbers, we may collect the following information:</p>
          <ul class="list-disc ml-6 mt-2">
            <li>Personal identification information (Name, Phone Number).</li>
            <li>Appointment details and booking history.</li>
            <li>Communication records (e.g., WhatsApp opt-in status and messages).</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">3. How We Use Your Information</h2>
          <p>We use the collected information primarily to:</p>
          <ul class="list-disc ml-6 mt-2">
            <li>Manage and confirm your barber appointments.</li>
            <li>Send you transactional notifications, reminders, and updates via WhatsApp using the official WhatsApp Business API.</li>
            <li>Improve our platform's user experience and customer service.</li>
          </ul>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">4. Data Storage and Security</h2>
          <p>In compliance with Moroccan data protection regulations (Law 09-08), your personal data is stored securely. We implement reasonable technical and organizational measures to protect your data against unauthorized access, alteration, or disclosure.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">5. Third-Party Services</h2>
          <p>We use Meta's WhatsApp Business API to deliver messages. Meta acts as a data processor for these messages. We do not sell, trade, or rent your personal identification information to others.</p>
        </section>

        <section>
          <h2 class="text-xl font-semibold text-white mb-2">6. Contact Us</h2>
          <p>If you have any questions about this Privacy Policy or wish to request the deletion of your data, please contact us at:</p>
          <p class="mt-2 text-yellow-500">Email: contact@[YOUR-DOMAIN].ma</p>
        </section>
      </div>
    </div>
  `
})
export class PrivacyPolicyComponent {}