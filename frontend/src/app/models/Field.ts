import { FieldStatus } from "./FieldStatus";
import { GrowthStage } from "./GrowthStage";
import { SeedType } from "./SeedType";

export class Field {
  id!: number;
  status!: FieldStatus;
  seedType?: SeedType;
  growthStage?: GrowthStage;
  plantedDate?: Date;
  harvestDate?: Date;
}
