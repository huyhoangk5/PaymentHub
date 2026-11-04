import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { TuiButton, TuiDialogContext, TuiTextfield } from '@taiga-ui/core';
import { TuiTextarea } from '@taiga-ui/kit';

export interface ConfirmDialogOptions {
  title: string;
  message?: string;
  showReasonInput?: boolean;
  reasonMaxLength?: number;
  confirmText?: string;
  cancelText?: string;
  isDanger?: boolean;
}

export interface ConfirmDialogResult {
  confirmed: boolean;
  reason?: string;
}

@Component({
  selector: 'app-confirm-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule, TuiButton, TuiTextfield, TuiTextarea],
  templateUrl: './confirm-dialog.component.html',
  styleUrls: ['./confirm-dialog.component.scss']
})
export class ConfirmDialogComponent {
  reason = '';

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<ConfirmDialogResult, ConfirmDialogOptions>
  ) {}

  get options(): ConfirmDialogOptions {
    return this.context.data;
  }

  get maxLength(): number {
    return this.options.reasonMaxLength || 250;
  }

  onConfirm(): void {
    if (this.options.showReasonInput && !this.reason.trim()) {
      return;
    }
    this.context.completeWith({
      confirmed: true,
      reason: this.reason.trim()
    });
  }

  onCancel(): void {
    this.context.completeWith({
      confirmed: false
    });
  }
}
