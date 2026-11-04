import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ApprovalStatus } from '../../../core/models/component.model';
import { StatusLabelPipe } from '../../pipes/status-label.pipe';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  imports: [CommonModule, StatusLabelPipe],
  templateUrl: './status-badge.component.html',
  styleUrls: ['./status-badge.component.scss']
})
export class StatusBadgeComponent {
  @Input({ required: true }) status!: ApprovalStatus | number;

  getStatusClass(): string {
    switch (this.status) {
      case ApprovalStatus.NEW:
        return 'badge-new';
      case ApprovalStatus.PENDING:
        return 'badge-pending';
      case ApprovalStatus.APPROVED:
        return 'badge-approved';
      case ApprovalStatus.REJECTED:
        return 'badge-rejected';
      case ApprovalStatus.CANCELLED:
        return 'badge-cancelled';
      default:
        return 'badge-default';
    }
  }
}
