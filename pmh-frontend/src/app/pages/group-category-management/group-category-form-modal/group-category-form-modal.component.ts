import { Component, Inject, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { POLYMORPHEUS_CONTEXT } from '@taiga-ui/polymorpheus';
import { TuiDay } from '@taiga-ui/cdk';
import {
  TuiAlertService,
  TuiButton,
  TuiDataList,
  TuiDialogContext,
  TuiFilterByInputPipe,
  TuiInput,
  TuiTextfield
} from '@taiga-ui/core';
import {
  TuiChevron,
  TuiInputChip,
  TuiInputDate,
  TuiMultiSelect,
  TuiTextarea
} from '@taiga-ui/kit';

import { GroupCategoryResponse, GroupCategoryCreateRequest, GroupCategoryUpdateRequest } from '../../../core/models/group-category.model';
import { GroupCategoryService } from '../../../core/services/group-category.service';
import { ComponentService } from '../../../core/services/component.service';
import { ComponentResponse } from '../../../core/models/component.model';

export interface GroupCategoryFormDialogOptions {
  mode: 'CREATE' | 'EDIT' | 'COPY';
  data?: GroupCategoryResponse;
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

function nonEmptyArrayValidator(control: AbstractControl): ValidationErrors | null {
  const val = control.value;
  if (Array.isArray(val) && val.length > 0) return null;
  return { required: true };
}

@Component({
  selector: 'app-group-category-form-modal',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    TuiTextfield,
    TuiInput,
    TuiInputChip,
    TuiMultiSelect,
    TuiDataList,
    TuiFilterByInputPipe,
    TuiInputDate,
    TuiTextarea,
    TuiButton,
    TuiChevron
  ],
  templateUrl: './group-category-form-modal.component.html',
  styleUrls: ['./group-category-form-modal.component.scss']
})
export class GroupCategoryFormModalComponent implements OnInit {
  private fb = inject(FormBuilder);
  private groupCategoryService = inject(GroupCategoryService);
  private componentService = inject(ComponentService);
  private alerts = inject(TuiAlertService);

  form!: FormGroup;
  loading = false;
  errorMessage = '';

  readonly minDate = TuiDay.currentLocal();

  componentsList: ComponentResponse[] = [];
  componentCodesList: string[] = [];

  constructor(
    @Inject(POLYMORPHEUS_CONTEXT)
    readonly context: TuiDialogContext<boolean, GroupCategoryFormDialogOptions>
  ) {}

  get options(): GroupCategoryFormDialogOptions {
    return this.context.data;
  }

  get isEdit(): boolean {
    return this.options.mode === 'EDIT';
  }

  get breadcrumbAction(): string {
    switch (this.options.mode) {
      case 'CREATE':
      case 'COPY':
        return 'Thêm mới';
      case 'EDIT':
        return 'Sửa';
    }
  }

  get title(): string {
    switch (this.options.mode) {
      case 'CREATE':
      case 'COPY':
        return 'Thêm mới tham số danh mục theo nhóm';
      case 'EDIT':
        return 'Sửa tham số theo nhóm';
    }
  }

  ngOnInit(): void {
    const item = this.options.data;

    const initialComponentCodes = item?.componentCode
      ? item.componentCode.split(',').map((s) => s.trim()).filter(Boolean)
      : [];

    const initialEffectiveDay = parseIsoToTuiDay(item?.effectiveDate) || TuiDay.currentLocal();
    const initialEndEffectiveDay = parseIsoToTuiDay(item?.endEffectiveDate);

    this.form = this.fb.group(
      {
        paramType: [
          { value: item?.paramType || '', disabled: this.isEdit },
          [Validators.required, Validators.maxLength(255)]
        ],
        paramValue: [
          { value: item?.paramValue || '', disabled: this.isEdit },
          [Validators.required, Validators.maxLength(255)]
        ],
        paramName: [
          item?.paramName || '',
          [Validators.required, Validators.maxLength(255)]
        ],
        componentCodes: [
          initialComponentCodes,
          [nonEmptyArrayValidator]
        ],
        effectiveDate: [
          initialEffectiveDay,
          [Validators.required, notPastDateValidator(this.isEdit ? item?.effectiveDate : null)]
        ],
        endEffectiveDate: [
          initialEndEffectiveDay,
          [notPastDateValidator(this.isEdit ? item?.endEffectiveDate : null)]
        ],
        description: [item?.description || '', [Validators.maxLength(4000)]],
        isActive: [item?.isActive !== undefined ? item.isActive : 1]
      },
      { validators: dateRangeValidator }
    );

    this.loadComponents(initialComponentCodes);
  }

  private loadComponents(existingCodes: string[]): void {
    // Theo RSD mục 5 trang 15: Lấy COMPONENT_CODE của bảng PMH_COMPONENTS với IS_ACTIVE = 1
    this.componentService.search({ page: 0, size: 500, isActive: 1 }).subscribe({
      next: (res) => {
        this.componentsList = res.data?.content || [];
        const codesFromApi = this.componentsList.map((c) => c.componentCode).filter(Boolean);
        this.componentCodesList = Array.from(new Set([...codesFromApi, ...existingCodes]));
      },
      error: () => {
        this.componentCodesList = [...existingCodes];
      }
    });
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
    const payload: GroupCategoryCreateRequest = {
      paramType: (raw.paramType || '').trim(),
      paramValue: (raw.paramValue || '').trim(),
      paramName: (raw.paramName || '').trim(),
      componentCode: Array.isArray(raw.componentCodes)
        ? raw.componentCodes.join(', ')
        : (raw.componentCodes || ''),
      effectiveDate: formatTuiDayToIso(raw.effectiveDate)!,
      endEffectiveDate: formatTuiDayToIso(raw.endEffectiveDate),
      description: raw.description ? raw.description.trim() : '',
      isActive: raw.isActive ?? 1
    };

    if (this.options.mode === 'EDIT' && this.options.data) {
      const updateReq: GroupCategoryUpdateRequest = {
        id: this.options.data.id,
        ...payload
      };
      this.groupCategoryService.update(this.options.data.id, updateReq).subscribe({
        next: (res) => {
          if (sendApprove && res.data?.id) {
            this.groupCategoryService.sendApprove(res.data.id).subscribe({
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
      this.groupCategoryService.create(payload).subscribe({
        next: (res) => {
          if (sendApprove && res.data?.id) {
            this.groupCategoryService.sendApprove(res.data.id).subscribe({
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
