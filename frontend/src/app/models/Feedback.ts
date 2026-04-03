export interface HarvestFeedback {
  id: string;
  farmName: string;
  cropType: string;
  cropIcon: string;
  harvestDate: string;
  status: 'locked' | 'ready' | 'completed';
  estimatedHarvest?: string;
  lockedUntil?: string;
  feedback?: FeedbackDetails;
}

export interface FeedbackDetails {
  seedQuality: number;
  irrigation: number;
  appRecommendations: number;
  overallExperience: number;
  comment?: string;
  submittedAt?: Date;
}
