/** Bottom sheet: title, editable fields list, Confirm/Cancel actions. */
export interface ConfirmSheetProps {
  /** Sheet title, e.g. "Log workout" */
  title: string;
  /** Editable fields; values render right-aligned in mono */
  fields: Array<{ label: string; value: string; suffix?: string }>;
  accent?: 'diet' | 'workout' | 'study' | 'sleep';
  /** Index of the field shown focused/selected (accent border); -1 = none */
  focusedField?: number;
  disabled?: boolean;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
  onFieldChange?: (index: number, value: string) => void;
}
export declare function ConfirmSheet(props: ConfirmSheetProps): JSX.Element;
