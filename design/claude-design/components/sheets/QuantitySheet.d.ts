/**
 * Bottom sheet: grams stepper with live macro readout.
 * @startingPoint section="Components" subtitle="Grams stepper + live macro readout bottom sheet" viewport="400x320"
 */
export interface QuantitySheetProps {
  brand?: string;
  /** Product name shown as sheet title */
  product: string;
  /** Macros per 100 g — readout scales live with grams */
  per100g: { kcal: number; protein: number; carbs: number; fat: number };
  initialGrams?: number;
  /** Stepper increment in grams (default 10) */
  step?: number;
  accent?: 'diet' | 'workout' | 'study' | 'sleep';
  /** Force the edited/selected visual (accent grams readout) */
  edited?: boolean;
  disabled?: boolean;
  onAdd?: (grams: number) => void;
  onCancel?: () => void;
}
export declare function QuantitySheet(props: QuantitySheetProps): JSX.Element;
