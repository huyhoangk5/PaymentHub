import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'activeLabel',
  standalone: true
})
export class ActiveLabelPipe implements PipeTransform {
  transform(isActive: number | boolean | null | undefined): string {
    if (isActive === 1 || isActive === true) return 'Hoạt động';
    if (isActive === 0 || isActive === false) return 'Không hoạt động';
    return '';
  }
}
