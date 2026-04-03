import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';
import {FarmService, HarvestRequest} from '../../services/farm-service/farm-service';
import { FieldStatus } from '../../models/FieldStatus';
import { GrowthStage } from '../../models/GrowthStage';
import { Field } from '../../models/Field';
import { SeedType } from '../../models/SeedType';
import {take} from 'rxjs/operators';


type GrowthStageIcons = Record<GrowthStage, string>;
type SeedIconMap = Record<SeedType, GrowthStageIcons>;


@Component({
  selector: 'app-field-grid',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './field-grid.html',
  styleUrl: './field-grid.css',
})
export class FieldGrid {
  fields: Field[] = [];

  public SeedType = SeedType;
  public FieldStatus = FieldStatus;
  public GrowthStage = GrowthStage;

  isModalOpen = false;
  isDetailsModalOpen = false;
  isHarvestModalOpen = false;
  selectedFieldId: number | null = null;
  selectedSeedType: SeedType | null = null;
  sowingDate: string = '';
  harvestDate: string = '';

  private seedGrowthRules: Record<string, { young: number, mature: number, ready: number }> = {
    [SeedType.wheat]: { young: 30, mature: 70, ready: 110 },
    [SeedType.corn]: { young: 25, mature: 60, ready: 100 },
    [SeedType.barley]: { young: 25, mature: 60, ready: 90 },
    [SeedType.pumpkin]: { young: 20, mature: 50, ready: 90 },
    [SeedType.whiteGrapes]: { young: 45, mature: 90, ready: 135 },
    [SeedType.blackGrapes]: { young: 45, mature: 90, ready: 135 }
  };


constructor(private farmService: FarmService, private toastr: ToastrService) {
  this.farmService.selectedFarm$.subscribe(farm => {
    this.fields = farm?.fields || [];
    console.log(this.fields);
  });
}

  devMode = false;

  seedIcons: SeedIconMap = {
    [SeedType.wheat]: {
      [GrowthStage.seedling]: 'assets/icons/plant.svg',
      [GrowthStage.young]: 'assets/icons/wheat_growing.svg',
      [GrowthStage.mature]: 'assets/icons/wheat_growing.svg',
      [GrowthStage.ready]: 'assets/icons/wheat_ready.svg',
    },
    [SeedType.corn]: {
      [GrowthStage.seedling]: 'assets/icons/plant.svg',
      [GrowthStage.young]: 'assets/icons/corn_growing.svg',
      [GrowthStage.mature]: 'assets/icons/corn_growing.svg',
      [GrowthStage.ready]: 'assets/icons/corn_ready.svg',
    },
    [SeedType.barley]: {
      [GrowthStage.seedling]: 'assets/icons/plant.svg',
      [GrowthStage.young]: 'assets/icons/barely_growing.svg',
      [GrowthStage.mature]: 'assets/icons/barely_growing.svg',
      [GrowthStage.ready]: 'assets/icons/barely_ready.svg',
    },
    [SeedType.blackGrapes]: {
      [GrowthStage.seedling]: 'assets/icons/plant.svg',
      [GrowthStage.young]: 'assets/icons/black_grape_growing.svg',
      [GrowthStage.mature]: 'assets/icons/black_grape_growing.svg',
      [GrowthStage.ready]: 'assets/icons/black_grape_ready.svg',
    },
    [SeedType.whiteGrapes]: {
      [GrowthStage.seedling]: 'assets/icons/plant.svg',
      [GrowthStage.young]: 'assets/icons/black_grape_growing.svg',
      [GrowthStage.mature]: 'assets/icons/black_grape_growing.svg',
      [GrowthStage.ready]: 'assets/icons/white_grape_ready.svg',
    },
    [SeedType.pumpkin]: {
      [GrowthStage.seedling]: 'assets/icons/plant.svg',
      [GrowthStage.young]: 'assets/icons/pumpkin_growing.svg',
      [GrowthStage.mature]: 'assets/icons/pumpkin_growing.svg',
      [GrowthStage.ready]: 'assets/icons/pumpkin_ready.svg',
    },
  };

  // ============================================
  // VALIDATION METHODS
  // ============================================

  private validateSeedSelection(): { valid: boolean; message?: string; title?: string } {
    if (!this.selectedSeedType) {
      return {
        valid: false,
        message: 'Please select a seed type to plant',
        title: 'Seed Type Missing'
      };
    }
    return { valid: true };
  }

  private calculateInitialGrowthStage(seedType: SeedType, sowingDateStr: string): GrowthStage {
    const planted = new Date(sowingDateStr);
    const now = new Date();

    planted.setHours(0, 0, 0, 0);
    now.setHours(0, 0, 0, 0);

    const diffTime = now.getTime() - planted.getTime();
    const daysElapsed = Math.ceil(diffTime / (1000 * 60 * 60 * 24));

    const rules = this.seedGrowthRules[seedType];

    if (!rules) return GrowthStage.seedling;

    if (daysElapsed >= rules.ready) {
      return GrowthStage.ready;
    } else if (daysElapsed >= rules.mature) {
      return GrowthStage.mature;
    } else if (daysElapsed >= rules.young) {
      return GrowthStage.young;
    } else {
      return GrowthStage.seedling;
    }
  }

  private validateSowingDate(dateString: string): { valid: boolean; message?: string; title?: string } {
    if (!dateString || dateString.trim() === '') {
      return {
        valid: false,
        message: 'Please select a sowing date before planting',
        title: 'Date Missing'
      };
    }

    const parts = dateString.split('-');
    const year = parseInt(parts[0], 10);
    const month = parseInt(parts[1], 10) - 1;
    const day = parseInt(parts[2], 10);
    const selectedDate = new Date(year, month, day);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (isNaN(selectedDate.getTime())) {
      return {
        valid: false,
        message: 'The selected date is invalid. Please choose a valid date',
        title: 'Invalid Date'
      };
    }

    if (selectedDate > today) {
      return {
        valid: false,
        message: 'Sowing date cannot be in the future',
        title: 'Future Date'
      };
    }

    const oneYearAgo = new Date();
    oneYearAgo.setFullYear(oneYearAgo.getFullYear() - 1);

    if (selectedDate < oneYearAgo) {
      return {
        valid: false,
        message: 'Sowing date cannot be more than 1 year in the past',
        title: 'Date Too Old'
      };
    }

    return { valid: true };
  }

  private validateHarvestDate(
    harvestDateString: string,
    plantedDate?: Date
  ): { valid: boolean; message?: string; title?: string } {
    if (!harvestDateString || harvestDateString.trim() === '') {
      return {
        valid: false,
        message: 'Please select a harvest date',
        title: 'Date Missing'
      };
    }

    const harvestDate = new Date(harvestDateString);
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    if (isNaN(harvestDate.getTime())) {
      return {
        valid: false,
        message: 'The selected harvest date is invalid',
        title: 'Invalid Date'
      };
    }

    if (plantedDate) {
      const plantedDateOnly = new Date(plantedDate);
      plantedDateOnly.setHours(0, 0, 0, 0);

      if (harvestDate < plantedDateOnly) {
        const plantedDateStr = plantedDate.toLocaleDateString();
        return {
          valid: false,
          message: `Harvest date cannot be before planting date (${plantedDateStr})`,
          title: 'Invalid Date Range'
        };
      }

      if (harvestDate.getTime() === plantedDateOnly.getTime()) {
        return {
          valid: false,
          message: 'Harvest date cannot be the same as planting date',
          title: 'Too Soon'
        };
      }
    }

    if (harvestDate > today) {
      return {
        valid: false,
        message: 'Harvest date cannot be in the future',
        title: 'Future Date'
      };
    }

    return { valid: true };
  }

  // ============================================
  // HELPER METHODS
  // ============================================

  get firstEmptyFieldId(): number | null {
    const emptyField = this.fields.find(f => f.status === FieldStatus.empty);
    return emptyField ? emptyField.id : null;
  }

  hasPlus(fieldId: number): boolean {
    return this.firstEmptyFieldId === fieldId;
  }

  onAnswerChange(questionId: string, event: any) {
    const selectedValue = event.target.value;

  }

  isPlanted(fieldId: number): boolean {
    const field = this.fields.find(f => f.id === fieldId);
    return field ? field.status !== FieldStatus.empty : false;
  }

  isReady(fieldId: number): boolean {
    const field = this.fields.find(f => f.id === fieldId);
    return field ? field.status === FieldStatus.ready : false;
  }

  getField(fieldId: number): Field | undefined {
    return this.fields.find(f => f.id === fieldId);
  }

  getPlantIcon(fieldId: number): string {
    const field = this.getField(fieldId);

    if (!field || field.status === FieldStatus.empty || !field.seedType) {
      return '';
    }

    const seedType = field.seedType;
    const growthStage = field.growthStage || GrowthStage.seedling;

    if (this.seedIcons[seedType] && this.seedIcons[seedType][growthStage]) {
      return this.seedIcons[seedType][growthStage];
    }

    return 'assets/icons/plant.svg';
  }

  getSelectedField(): Field | undefined {
    if (this.selectedFieldId) {
      return this.getField(this.selectedFieldId);
    }
    return undefined;
  }

  formatDate(date: Date | undefined): string {
    if (!date) return 'N/A';
    return new Date(date).toLocaleDateString();
  }

  getFieldStatusDisplay(fieldId: number): string {
    const field = this.getField(fieldId);
    if (!field) return '';

    if (field.status === FieldStatus.empty) return 'Empty';

    return `${field.status} (${field.growthStage}) - ${field.seedType}`;
  }

  // ============================================
  // MODAL METHODS
  // ============================================

  openAddSeedModal(fieldId: number) {
    // Close all other modals first
    this.isDetailsModalOpen = false;
    this.isHarvestModalOpen = false;

    this.selectedFieldId = fieldId;
    this.selectedSeedType = null;
    this.sowingDate = '';
    this.isModalOpen = true;
  }

  openFieldDetailsModal(fieldId: number) {
    const field = this.getField(fieldId);

    if (field && field.status === FieldStatus.ready) {
      this.openHarvestModal(fieldId);
      return;
    }

    if (field && field.status !== FieldStatus.empty) {
      // Close all other modals first
      this.isModalOpen = false;
      this.isHarvestModalOpen = false;

      this.selectedFieldId = fieldId;
      this.isDetailsModalOpen = true;
    }
  }

  openHarvestModal(fieldId: number) {
    // Close all other modals first
    this.isModalOpen = false;
    this.isDetailsModalOpen = false;

    this.selectedFieldId = fieldId;
    this.harvestDate = new Date().toISOString().split('T')[0];
    this.isHarvestModalOpen = true;
  }

  closeModal() {
    this.isModalOpen = false;
    this.selectedFieldId = null;
    this.selectedSeedType = null;
    this.sowingDate = '';
  }

  closeDetailsModal() {
    this.isDetailsModalOpen = false;
    this.selectedFieldId = null;
  }

  closeHarvestModal() {
    this.isHarvestModalOpen = false;
    this.selectedFieldId = null;
    this.harvestDate = '';
  }

  selectSeedType(seedType: SeedType) {
    this.selectedSeedType = seedType;
  }

  // ============================================
  // FIELD ACTIONS WITH VALIDATION
  // ============================================

  addSeedConfirm() {
    // Validate seed type
    const seedValidation = this.validateSeedSelection();
    if (!seedValidation.valid) {
      this.toastr.warning(seedValidation.message!, seedValidation.title!);
      return;
    }

    // Validate sowing date
    const dateValidation = this.validateSowingDate(this.sowingDate);
    if (!dateValidation.valid) {
      this.toastr.error(dateValidation.message!, dateValidation.title!);
      return;
    }

    // All validations passed - plant the field
    if (this.selectedFieldId) {
      const fieldIndex = this.fields.findIndex(f => f.id === this.selectedFieldId);
      if (fieldIndex !== -1) {
        // let finalPlantedDate = new Date(this.sowingDate);
        const parts = this.sowingDate.split('-');
        const year = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10) - 1;
        const day = parseInt(parts[2], 10);

        let finalPlantedDate = new Date(year, month, day);
        const today = new Date();

        const isToday = finalPlantedDate.getDate() === today.getDate() &&
          finalPlantedDate.getMonth() === today.getMonth() &&
          finalPlantedDate.getFullYear() === today.getFullYear();

        if (isToday) {
          finalPlantedDate.setHours(today.getHours(), today.getMinutes(), today.getSeconds());
        } else {
          finalPlantedDate.setHours(0, 0, 0);
        }

        const calculatedStage = this.calculateInitialGrowthStage(
          this.selectedSeedType!,
          finalPlantedDate.toISOString()
        );

        const fieldUpdate = {
          id: this.selectedFieldId,
          status: calculatedStage === GrowthStage.ready ? FieldStatus.ready : FieldStatus.planted,
          seedType: this.selectedSeedType!,
          plantedDate: finalPlantedDate,
          growthStage: calculatedStage
        };

        // Capture seedType before async call (closeModal() will set it to null)
        const seedType = this.selectedSeedType!;

        this.farmService.updateField(fieldUpdate).subscribe({
          next: () => {
            // Success notification
            const seedName = seedType.replace('_', ' ');
            this.toastr.success(
              `${seedName.charAt(0).toUpperCase() + seedName.slice(1)} planted successfully`,
              `Field ${this.selectedFieldId} Planted`
            );
          },
          error: () => this.toastr.error('Update failed')
        });
      }
    }
    this.closeModal();
  }

  harvestField() {
    if (this.selectedFieldId) {
      const field = this.getField(this.selectedFieldId);

      // Validate harvest date
      const dateValidation = this.validateHarvestDate(this.harvestDate, field?.plantedDate);
      // if (!dateValidation.valid) {
      //   this.toastr.error(dateValidation.message!, dateValidation.title!);
      //   return;
      // }

      // All validations passed - harvest the field
      this.farmService.selectedFarm$.pipe(take(1)).subscribe(farm => {
        if (farm) {

          const harvestPayload: HarvestRequest = {
            harvestDate: new Date(this.harvestDate),
            answers: []
          };

          this.farmService.harvestField(farm.id, this.selectedFieldId!, harvestPayload).subscribe({
            next: () => {
              const cropName = field?.seedType?.replace('_', ' ');
              this.toastr.success(
                `${cropName} harvested! Check Feedback page later.`,
                `Field ${this.selectedFieldId} Reset`
              );

              const index = this.fields.findIndex(f => f.id === this.selectedFieldId);
              if (index !== -1) {
                this.fields[index] = {
                  id: this.selectedFieldId!,
                  status: FieldStatus.empty,
                };
              }

              this.closeHarvestModal();
            },
            error: (err) => {
              console.error(err);
              this.toastr.error('Failed to record harvest', 'Error');
            }
          });
        }
      });
    }
  }

  // ============================================
  // DEVELOPER CONTROLS
  // ============================================

  advanceGrowth(fieldId: number) {
    const field = this.getField(fieldId);
    if (!field || field.status === FieldStatus.empty) return;

    const fieldIndex = this.fields.findIndex(f => f.id === fieldId);

    if (field.status === FieldStatus.planted && field.growthStage === GrowthStage.seedling) {
      this.fields[fieldIndex] = {
        ...field,
        status: FieldStatus.growing,
        growthStage: GrowthStage.young
      };
      this.toastr.info(`Field ${fieldId} is now growing`, 'Growth Stage: Young');

    } else if (field.status === FieldStatus.growing && field.growthStage === GrowthStage.young) {
      this.fields[fieldIndex] = {
        ...field,
        growthStage: GrowthStage.mature
      };
      this.toastr.info(`Field ${fieldId} is maturing`, 'Growth Stage: Mature');

    } else if (field.status === FieldStatus.growing && field.growthStage === GrowthStage.mature) {
      this.fields[fieldIndex] = {
        ...field,
        status: FieldStatus.ready,
        growthStage: GrowthStage.ready
      };
      const cropName = field.seedType?.replace('_', ' ');
      this.toastr.success(
        `${cropName?.charAt(0).toUpperCase()}${cropName?.slice(1)} is ready to harvest`,
        `Field ${fieldId} Ready`
      );
    }
  }

  resetField(fieldId: number) {
    const field = this.getField(fieldId);
    const fieldIndex = this.fields.findIndex(f => f.id === fieldId);

    if (fieldIndex !== -1 && field && field.status !== FieldStatus.empty) {
      const cropName = field.seedType?.replace('_', ' ');

      this.fields[fieldIndex] = {
        id: fieldId,
        status: FieldStatus.empty
      };

      this.toastr.info(
        `${cropName ? cropName.charAt(0).toUpperCase() + cropName.slice(1) + ' removed. ' : ''}Field is now empty`,
        `Field ${fieldId} Reset`
      );
    }
  }
}
