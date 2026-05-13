import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  template: `
    <footer class="bg-gray-900 text-gray-300 py-8 border-t border-gray-800 text-sm mt-10">
      <div class="container mx-auto px-4 flex flex-col md:flex-row justify-between items-center gap-4">
        <div class="flex flex-col text-center md:text-left">
          <h3 class="text-white font-bold text-lg mb-2">T7li9a</h3>
          <p>© 2026 T7li9a. Operated by <span class="text-yellow-500">[SMITEK]</span>.</p>
          <p>Address: <span class="text-yellow-500">[ADRISA]</span></p>
          <p class="mt-2">
            Email: <span class="text-yellow-500">contact@[DOMAINE].ma</span> | 
            Phone: <span class="text-yellow-500">[NEMRA]</span>
          </p>
        </div>
        <div class="flex flex-col md:flex-row gap-4 md:gap-8 font-medium">
          <a routerLink="/privacy-policy" class="hover:text-white">Privacy Policy</a>
          <a routerLink="/terms" class="hover:text-white">Terms of Service</a>
        </div>
      </div>
    </footer>
  `
})
export class FooterComponent {}
