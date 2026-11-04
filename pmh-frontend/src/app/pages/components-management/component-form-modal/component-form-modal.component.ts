import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { TuiDay } from '@taiga-ui/cdk';
import {
  TuiAlertService,
  TuiButton,
  TuiCheckbox,
  TuiDataList,
  TuiDialogContext,
  TuiFilterByInputPipe,
  TuiInput,
  TuiTextfield
} from '@taiga-ui/core';
import {
  TuiChevron,
  TuiDataListWrapper,
  TuiInputChip,
  TuiInputDate,
  TuiMultiSelect
} from '@taiga-ui/kit';
import { ComponentResponse, ComponentCreateRequest, ComponentUpdateRequest } from '../../../core/models/component.model';
import { ComponentService } from '../../../core/services/component.service';

export interface ComponentFormDialogOptions {
  mode: 'CREATE' | 'EDIT' | 'COPY';
  data?: ComponentResponse;
}

// Chặn ký tự đặc biệt theo RSD: ^, #, |, *, @, $, `, ~, !, %, &, {, }, [, ], ?, <, >, ", ', (,), /, :, ;, \, =, dấu phẩy
const FORBIDDEN_SPECIAL_CHARS_REGEX = /[\^#|*@$₩`~!%&{}\[\]?<>"'()/:;\\=,]/;

function noSpecialCharsValidator(control: AbstractControl): ValidationErrors | null {
  const val = control.value;
  if (!val) return null;
  return FORBIDDEN_SPECIAL_CHARS_REGEX.test(val) ? { specialChars: true } : null;
}

function componentCodeValidator(control: AbstractControl): ValidationErrors | null {
  const val = control.value;
  if (!val) return null;
  if (!/^[A-Z0-9_]+$/.test(val)) {
    return { invalidCode: true };
  }
  return null;
}

function notPastDateValidator(allowExistingDateStr?: string | null) {
  return (control: AbstractControl): ValidationErrors | null => {
    const val: TuiDay | null = control.value;
    if (!val || !(val instanceof TuiDay)) return null;
    const today = TuiDay.currentLocal();
    if (allowExistingDateStr) {
      const existing = parseIsoToTuiDay(allowExistingDateStr);
      if (existing && val.daySame(existing)) {
        return null;
      }
    }
    if (val.dayBefore(today)) {
      return { pastDate: true };
    }
    return null;
  };
}

function dateRangeValidator(group: AbstractControl): ValidationErrors | null {
  const start = group.get('effectiveDate')?.value as TuiDay | null;
  const end = group.get('endEffectiveDate')?.value as TuiDay | null;
  if (start instanceof TuiDay && end instanceof TuiDay) {
    if (end.dayBefore(start)) {
      return { endBeforeStart: true };
    }
  }
  return null;
}

function parseIsoToTuiDay(isoStr?: string | null): TuiDay | null {
  if (!isoStr) return null;
  const clean = isoStr.split('T')[0];
  const parts = clean.split('-');
  if (parts.length === 3) {
    const y = Number(parts[0]);
    const m = Number(parts[1]) - 1;
    const d = Number(parts[2]);
    if (!isNaN(y) && !isNaN(m) && !isNaN(d)) {
      return new TuiDay(y, m, d);
    }
  }
  return null;
}

function formatTuiDayToIso(day: TuiDay | null | undefined): string | undefined {
  if (!day || !(day instanceof TuiDay)) return undefined;
  const y = String(day.year).padStart(4, '0');
  const m = String(day.month + 1).padStart(2, '0');
  const d = String(day.day).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

@Component({
  selector: 'app-component-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TuiTextfield,
    TuiInput,
    TuiInputChip,
    TuiMultiSelect,
    TuiDataList,
    TuiDataListWrapper,
    TuiFilterByInputPipe,
    TuiInputDate,
    TuiCheckbox,
    TuiButton,
    TuiChevron
  ],
  templateUrl: './component-form-modal.component.html',
  styleUrls: ['./component-form-modal.component.scss']
})
export class ComponentFormModalComponent implements OnInit {
  private fb = inject(FormBuilder);
  private componentService = inject(ComponentService);
  private alerts = inject(TuiAlertService);

  form!: FormGroup;
  loading = false;
  errorMessage = '';

  readonly minDate = TuiDay.currentLocal();

  // Danh sách Chuẩn tin điện & Tên kết nối lấy từ API distinct PMH_COMPONENTS
  messageTypes: string[] = [];
  connectionMethods: string[] = [];

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<boolean, ComponentFormDialogOptions>
  ) {}

  get options(): ComponentFormDialogOptions {
    return this.context.data;
  }

  get isEdit(): boolean {
    return this.options.mode === 'EDIT';
  }

  get breadcrumbAction(): string {
    switch (this.options.mode) {
      case 'CREATE':
        return 'Thêm mới';
      case 'EDIT':
        return 'Sửa';
      case 'COPY':
        return 'Thêm mới';
    }
  }

  get title(): string {
    switch (this.options.mode) {
      case 'CREATE':
      case 'COPY':
        return 'Thêm mới tham số cấu phần xử lý';
      case 'EDIT':
        return 'Sửa tham số cấu phần xử lý';
    }
  }

  ngOnInit(): void {
    const item = this.options.data;

    const initialMessageTypes = item?.messageType
      ? item.messageType.split(',').map((s) => s.trim()).filter(Boolean)
      : [];
    const initialConnectionMethods = item?.connectionMethod
      ? item.connectionMethod.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

    this.messageTypes = [...initialMessageTypes];
    this.connectionMethods = [...initialConnectionMethods];

    const initialEffectiveDay = parseIsoToTuiDay(item?.effectiveDate) || TuiDay.currentLocal();
    const initialEndEffectiveDay = parseIsoToTuiDay(item?.endEffectiveDate);

    this.form = this.fb.group(
      {
        componentCode: [
          { value: item?.componentCode || '', disabled: this.isEdit },
          [Validators.required, Validators.maxLength(20), componentCodeValidator]
        ],
        componentName: [
          item?.componentName || '',
          [Validators.required, Validators.maxLength(150), noSpecialCharsValidator]
        ],
        messageTypes: [initialMessageTypes],
        connectionMethods: [initialConnectionMethods],
        effectiveDate: [
          initialEffectiveDay,
          [Validators.required, notPastDateValidator(this.isEdit ? item?.effectiveDate : null)]
        ],
        endEffectiveDate: [
          initialEndEffectiveDay,
          [notPastDateValidator(this.isEdit ? item?.endEffectiveDate : null)]
        ],
        checkToken: [item?.checkToken ?? false]
      },
      { validators: dateRangeValidator }
    );

    this.loadDistinctOptions(initialMessageTypes, initialConnectionMethods);
  }

  private loadDistinctOptions(initialMsg: string[], initialConn: string[]): void {
    this.componentService.getDistinctOptions().subscribe({
      next: (res) => {
        const msgFromDb = res.data?.messageTypes || [];
        const connFromDb = res.data?.connectionMethods || [];

        const mergedMsg = Array.from(new Set([...msgFromDb, ...initialMsg])).filter(Boolean);
        const mergedConn = Array.from(new Set([...connFromDb, ...initialConn])).filter(Boolean);

        this.messageTypes = mergedMsg;
        this.connectionMethods = mergedConn;
      },
      error: () => {
        // Fallback nếu API lỗi vẫn giữ các giá trị hiện tại
      }
    });
  }

  onComponentCodeInput(event: Event): void {
    const inputEl = event.target as HTMLInputElement;
    const upper = inputEl.value.toUpperCase();
    if (inputEl.value !== upper) {
      this.form.get('componentCode')?.setValue(upper, { emitEvent: false });
      inputEl.value = upper;
    }
  }

  close(): void {
    this.context.completeWith(false);
  }

  save(sendApprove = false): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.loading = true;
    this.errorMessage = '';

    const raw = this.form.getRawValue();
    const payload: ComponentCreateRequest = {
      componentCode: (raw.componentCode || '').trim().toUpperCase(),
      componentName: (raw.componentName || '').trim(),
      messageType: Array.isArray(raw.messageTypes) ? raw.messageTypes.join(', ') : (raw.messageTypes || ''),
      connectionMethod: Array.isArray(raw.connectionMethods) ? raw.connectionMethods.join(', ') : (raw.connectionMethods || ''),
      effectiveDate: formatTuiDayToIso(raw.effectiveDate)!,
      endEffectiveDate: formatTuiDayToIso(raw.endEffectiveDate),
      checkToken: !!raw.checkToken
    };

    if (this.options.mode === 'EDIT' && this.options.data) {
      const updateReq: ComponentUpdateRequest = {
        id: this.options.data.id,
        ...payload
      };
      this.componentService.update(this.options.data.id, updateReq).subscribe({
        next: (res) => {
          if (sendApprove && res.data?.id) {
            this.componentService.sendApprove(res.data.id).subscribe({
              next: () => {
                this.loading = false;
                this.alerts.open('Lưu và gửi duyệt thành công!', { appearance: 'positive' }).subscribe();
                this.context.completeWith(true);
              },
              error: (err) => this.handleError(err)
            });
          } else {
            this.loading = false;
            this.alerts.open('Lưu thành công!', { appearance: 'positive' }).subscribe();
            this.context.completeWith(true);
          }
        },
        error: (err) => this.handleError(err)
      });
    } else {
      this.componentService.create(payload).subscribe({
        next: (res) => {
          if (sendApprove && res.data?.id) {
            this.componentService.sendApprove(res.data.id).subscribe({
              next: () => {
                this.loading = false;
                this.alerts.open('Thêm mới và gửi duyệt thành công!', { appearance: 'positive' }).subscribe();
                this.context.completeWith(true);
              },
              error: (err) => this.handleError(err)
            });
          } else {
            this.loading = false;
            this.alerts.open('Thêm mới thành công!', { appearance: 'positive' }).subscribe();
            this.context.completeWith(true);
          }
        },
        error: (err) => this.handleError(err)
      });
    }
  }

  private handleError(err: any): void {
    this.loading = false;
    this.errorMessage = err?.error?.message || 'Có lỗi xảy ra, vui lòng thử lại!';
    this.alerts.open(this.errorMessage, { appearance: 'negative' }).subscribe();
  }
}
