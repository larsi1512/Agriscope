import { FieldStatus } from "../models/FieldStatus";
import { GrowthStage } from "../models/GrowthStage";
import { SeedType } from "../models/SeedType";

export class FieldDetailsDto {
  id!: number;
  status!: FieldStatus;
  seedType?: SeedType;
  plantedDate?: Date;
  harvestDate?: Date;
  growthStage?: GrowthStage;
}

export class FieldCreateDto {
  id!: number;
  status!: FieldStatus;
}
