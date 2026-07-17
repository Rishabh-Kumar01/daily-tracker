/**
 * Home-screen entry card for one activity type.
 * @startingPoint section="Components" subtitle="Activity entry card with icon + today summary" viewport="400x88"
 */
export interface ActivityCardProps {
  /** Activity key — drives accent color, default icon, default name */
  activity?: 'diet' | 'workout' | 'study' | 'sleep';
  /** Override display name (defaults from activity) */
  name?: string;
  /** Override Material Symbols icon name (defaults from activity) */
  icon?: string;
  /** One-line today summary, e.g. "1,840 kcal · 132g protein" */
  summary: string;
  selected?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}
export declare function ActivityCard(props: ActivityCardProps): JSX.Element;
