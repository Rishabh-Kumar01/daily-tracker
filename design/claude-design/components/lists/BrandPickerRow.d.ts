/** Product search-result row: thumbnail, brand + product name, per-100g macro line. */
export interface BrandPickerRowProps {
  /** Brand name, e.g. "Fage" (renders uppercase caption) */
  brand: string;
  /** Product name, e.g. "Total 0% Greek Yogurt" */
  product: string;
  /** Per-100g macro line, e.g. "per 100g · 54 kcal · 10.3P · 3.0C · 0.2F" */
  per100g: string;
  /** Product photo URL; striped placeholder when absent */
  thumbnailUrl?: string;
  accent?: 'diet' | 'workout' | 'study' | 'sleep';
  selected?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}
export declare function BrandPickerRow(props: BrandPickerRowProps): JSX.Element;
