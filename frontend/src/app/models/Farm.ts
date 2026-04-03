import { Field } from "./Field";
import { Recommendation } from "./Recommendation";
import { SoilType } from "./SoilType";

export class Farm {
    id : string = "";
    name : string = "";
    latitude : Number = 0;
    longitude : Number  = 0;
    soilType : SoilType = 0;
    fields : Field[] = [];
    recomendations : Recommendation[] = [];
    userId?: string;
    //TODO: stats ?
}