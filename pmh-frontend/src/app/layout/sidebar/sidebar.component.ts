import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

interface MenuItem {
  label: string;
  route?: string;
  icon?: string;
  permission?: string;
  disabled?: boolean;
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar.component.html',
  styleUrls: ['./sidebar.component.scss']
})
export class SidebarComponent {
  menuItems: MenuItem[] = [
    {
      label: 'Tham số Cấu phần xử lý',
      route: '/components',
      icon: 'lucide lucide-settings',
      permission: 'COMPONENT_VIEW'
    },
    {
      label: 'Tham số danh mục theo nhóm',
      route: '/group-categories',
      icon: 'lucide lucide-folder',
      permission: 'GROUP_CATEGORY_VIEW'
    },
    {
      label: 'Lịch sử giao dịch',
      route: '/transactions',
      icon: 'lucide lucide-file-Spreadsheet',
      permission: 'TRANSACTION_EXPORT'
    },
    {
      label: 'Quản lý phân quyền',
      route: '/permissions-management',
      icon: 'lucide lucide-shield-check'
    }
  ];
}
