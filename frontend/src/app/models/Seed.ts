import { SeedType } from "./SeedType";

export interface Seed {
  id: string;
  seedType: string;
  displayName: string;
  scientificName: string;

  // Temperature
  minTemperature: number;
  maxTemperature: number;

  // Growth timeline - ADD THESE FIELDS
  daysToYoung: number;
  daysToMature: number;
  daysToReady: number;

  // Water & Soil
  waterRequirement: number;
  minSoilMoisture: number;
  allowedWaterDeficit: number;

  // Stress thresholds
  heatStressTemperature: number;
  heavyRainThreshold: number;
  maxWindTolerance: number;

  // Disease risk
  diseaseRiskMinTemp: number;
  diseaseRiskMaxTemp: number;
  diseaseRainThreshold: number;

  // Other
  seedCoefficient: number;
}
