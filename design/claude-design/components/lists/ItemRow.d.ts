/** Checkable list row with right-aligned secondary value. */
export interface ItemRowProps {
  /** Item name, e.g. "Chicken breast" */
  name: string;
  /** Right-aligned secondary value, e.g. "180 g" or "264 kcal" */
  value?: string;
  /** Checked = selected state (accent checkbox, strikethrough name) */
  checked?: boolean;
  disabled?: boolean;
  /** Accent hue for the checkbox */
  accent?: 'diet' | 'workout' | 'study' | 'sleep';
  onChange?: (checked: boolean) => void;
}
export declare function ItemRow(props: ItemRowProps): JSX.Element;
