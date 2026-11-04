import { Pipe, PipeTransform } from '@angular/core';
import { ApprovalStatus } from '../../core/models/component.model';
import { APPROVAL_STATUS_LABEL } from '../../core/constants/approval-status.enum';

@Pipe({
  name: 'statusLabel',
  standalone: true
})
export class StatusLabelPipe implements PipeTransform {
  transform(status: ApprovalStatus | number | null | undefined): string {
    if (status === null || status === undefined) return '';
    return APPROVAL_STATUS_LABEL[status as ApprovalStatus] || 'Không xác định';
  }
}
