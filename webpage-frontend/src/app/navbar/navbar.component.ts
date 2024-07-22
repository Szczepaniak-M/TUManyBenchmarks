import { Component } from '@angular/core';

@Component({
  selector: 'app-navbar',
  template: `
    <nav class="bg-gray-800 p-4">
      <div class="container mx-auto flex justify-between items-center">
        <a href="/" class="text-white text-lg font-bold">
          TUManyBenchmarks
        </a>
        <div class="space-x-4">
          <a routerLink="/" routerLinkActive="text-blue-400" class="text-gray-300 hover:text-white">
            Home
          </a>
          <a routerLink="/explore" routerLinkActive="text-blue-400" class="text-gray-300 hover:text-white">
            Explore
          </a>
          <a routerLink="/about" routerLinkActive="text-blue-400" class="text-gray-300 hover:text-white">
            About
          </a>
        </div>
      </div>
    </nav>
  `
})
export class NavbarComponent {

}
