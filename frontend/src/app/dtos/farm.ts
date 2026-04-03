import { Recommendation } from "../models/Recommendation";
import { SoilType } from "../models/SoilType";
import { FieldCreateDto, FieldDetailsDto } from "./field";

export class FarmDetailsDto {
    id!: string;
    name!: string;
    latitude!: number;
    longitude!: number;
    soilType!: SoilType;
    fields!: FieldDetailsDto[];
    recommendations!: Recommendation[]; //TODO: Dto
    userId!: string;
}

export class FarmCreateDto {
    name!: string;
    latitude!: number;
    longitude!: number;
    soilType!: SoilType;
    fields!: FieldCreateDto[];
}

export interface FarmCheckResponse {
  hasFarms: boolean;
  farmCount: number;
}
