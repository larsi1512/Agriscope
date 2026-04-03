import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ToastrService } from 'ngx-toastr';

import { TopbarComponent } from '../topbar/topbar';
import { Sidebar } from '../sidebar/sidebar';
import {FarmService} from '../../services/farm-service/farm-service';
import {take} from 'rxjs/operators';

interface HarvestFeedback {
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

interface FeedbackDetails {
  submittedAt: Date;
  answers: FeedbackAnswer[];
}

interface FeedbackQuestion {
  id: string;
  question: string;
  targetParameter: string;
  description: string;
  options: FeedbackOption[];
}

interface FeedbackOption {
  label: string;
  value: number;
  multiplier: number;
}

interface FeedbackAnswer {
  questionId: string;
  selectedOption: FeedbackOption;
}

@Component({
  selector: 'app-feedback',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    TopbarComponent,
    Sidebar,
  ],
  templateUrl: './feedback.html',
  styleUrls: ['./feedback.css']
})
export class Feedback implements OnInit {
  harvests: HarvestFeedback[] = [];
  showFeedbackModal = false;
  showViewFeedbackModal = false;
  selectedHarvest: HarvestFeedback | null = null;

  // Question-based feedback
  feedbackQuestions: FeedbackQuestion[] = [];
  currentQuestionIndex = 0;
  userAnswers: FeedbackAnswer[] = [];
  selectedOption: FeedbackOption | null = null;
  currentFarmName: string = '';

  constructor(private farmService: FarmService,  private toastr: ToastrService) {}

  ngOnInit(): void {
    this.loadFeedbackQuestions();

    this.farmService.selectedFarm$.subscribe(farm => {
      if (farm && farm.id) {
        this.currentFarmName = farm.name;
        this.loadRealData(farm);
      } else {
        this.harvests = [];
        this.currentFarmName = '';
      }
    });
  }

  /**
   * Load feedback questions from JSON
   */
  loadFeedbackQuestions(): void {
    this.farmService.getFeedbackQuestions().subscribe({
      next: (data) => {
        this.feedbackQuestions = data;
        console.log('Questions loaded:', this.feedbackQuestions);
      },
      error: (err) => console.error('Could not load feedback questions', err)
    });
  }

  /**
   * Get current question
   */
  getCurrentQuestion(): FeedbackQuestion | null {
    if (this.currentQuestionIndex < this.feedbackQuestions.length) {
      return this.feedbackQuestions[this.currentQuestionIndex];
    }
    return null;
  }

  /**
   * Check if answer was already given for current question
   */
  getPreviousAnswer(): FeedbackOption | null {
    const currentQuestion = this.getCurrentQuestion();
    if (!currentQuestion) return null;

    const previousAnswer = this.userAnswers.find(
      answer => answer.questionId === currentQuestion.id
    );

    return previousAnswer ? previousAnswer.selectedOption : null;
  }

  /**
   * Get CSS class for card based on status
   */
  getCardClass(status: string): string {
    switch(status) {
      case 'ready': return 'harvest-card harvest-ready';
      case 'completed': return 'harvest-card harvest-completed';
      case 'locked': return 'harvest-card harvest-locked';
      default: return 'harvest-card';
    }
  }

  /**
   * Open feedback modal for adding new feedback
   */
  openFeedbackModal(harvest: HarvestFeedback): void {
    if (harvest.status === 'ready') {
      this.selectedHarvest = harvest;
      this.showFeedbackModal = true;
      this.currentQuestionIndex = 0;
      this.userAnswers = [];
      this.selectedOption = this.getPreviousAnswer();
    }
  }

  /**
   * View completed feedback (questions and answers)
   */
  viewFeedback(harvest: HarvestFeedback): void {
    if (harvest.status === 'completed' && harvest.feedback) {
      this.selectedHarvest = harvest;
      this.showViewFeedbackModal = true;
    }
  }

  /**
   * Close feedback modals
   */
  closeFeedbackModal(): void {
    this.showFeedbackModal = false;
    this.showViewFeedbackModal = false;
    this.selectedHarvest = null;
    this.currentQuestionIndex = 0;
    this.userAnswers = [];
    this.selectedOption = null;
  }

  /**
   * Select an option for current question
   */
  selectOption(option: FeedbackOption): void {
    this.selectedOption = option;
  }

  /**
   * Check if option is selected
   */
  isOptionSelected(option: FeedbackOption): boolean {
    return this.selectedOption === option;
  }

  /**
   * Save current answer and go to next question
   */
  nextQuestion(): void {
    if (!this.selectedOption) return;

    const currentQuestion = this.getCurrentQuestion();
    if (!currentQuestion) return;

    // Save or update answer
    const existingAnswerIndex = this.userAnswers.findIndex(
      answer => answer.questionId === currentQuestion.id
    );

    if (existingAnswerIndex >= 0) {
      this.userAnswers[existingAnswerIndex].selectedOption = this.selectedOption;
    } else {
      this.userAnswers.push({
        questionId: currentQuestion.id,
        selectedOption: this.selectedOption
      });
    }

    // Move to next question
    if (this.currentQuestionIndex < this.feedbackQuestions.length - 1) {
      this.currentQuestionIndex++;
      this.selectedOption = this.getPreviousAnswer();
    } else {
      // Last question - submit
      this.submitFeedback();
    }
  }

  loadRealData(farm: any): void {
    this.farmService.getHarvestHistory(farm.id).subscribe({
      next: (historyData) => {
        console.log('Harvest History loaded:', historyData);

        const historyHarvests: HarvestFeedback[] = historyData.map(h => {
          const hasFeedback = h.feedbackAnswers && h.feedbackAnswers.length > 0;
          return {
            id: h.id,
            farmName: farm.name,
            cropType: this.formatSeedName(h.seedType),
            cropIcon: this.getIconForSeed(h.seedType),
            harvestDate: new Date(h.harvestDate).toLocaleDateString('en-GB'),
            status: hasFeedback ? 'completed' : 'ready',
            feedback: hasFeedback ? {
              submittedAt: new Date(),
              answers: h.feedbackAnswers.map((ans: any) => ({
                questionId: ans.questionId,
                selectedOption: {
                  label: ans.answerLabel,
                  value: ans.answerValue,
                  multiplier: ans.multiplier
                }
              }))
            } : undefined
          };
        });

        const activeFields: HarvestFeedback[] = (farm.fields || [])
          .filter((f: any) => f.status !== 'EMPTY')
          .map((f: any) => {
            return {
              id: `field-${f.id}`,
              farmName: farm.name,
              cropType: this.formatSeedName(f.seedType || ''),
              cropIcon: this.getIconForSeed(this.formatSeedName(f.seedType as string)),
              harvestDate: '',
              status: 'locked',
              estimatedHarvest: f.plantedDate ?
                `Planted: ${new Date(f.plantedDate).toLocaleDateString('en-GB')}` :
                'Growing...',
              lockedUntil: 'TBD'
            } as HarvestFeedback;
          });

        this.harvests = [...activeFields, ...historyHarvests];

      },
      error: (err) => console.error('Failed to load harvest history', err)
    });
  }

  private getIconForSeed(seedType: string): string {
    if (!seedType) return 'plant.svg';
    switch (seedType.toLowerCase()) {
      case 'wheat':
        return 'wheat.svg';
      case 'corn':
        return 'corn.svg';
      case 'barley':
        return 'barely.svg';
      case 'pumpkin':
        return 'pumpkin.svg';
      case 'white grapes':
        return 'white_grape.svg';
      case 'black grapes':
        return 'grape.svg';
      default:
        return 'plant.svg';
    }
  }

  private formatSeedName(seedType: string): string {
    if (!seedType) return 'Crop';
    return seedType.replace(/_/g, ' ')
      .toLowerCase()
      .replace(/\b\w/g, c => c.toUpperCase());
  }

  /**
   * Go to previous question
   */
  previousQuestion(): void {
    if (this.currentQuestionIndex > 0) {
      this.currentQuestionIndex--;
      this.selectedOption = this.getPreviousAnswer();
    }
  }

  /**
   * Check if we're on first question
   */
  isFirstQuestion(): boolean {
    return this.currentQuestionIndex === 0;
  }

  /**
   * Check if we're on last question
   */
  isLastQuestion(): boolean {
    return this.currentQuestionIndex === this.feedbackQuestions.length - 1;
  }

  /**
   * Get progress percentage
   */
  getProgressPercentage(): number {
    return ((this.currentQuestionIndex + 1) / this.feedbackQuestions.length) * 100;
  }

  /**
   * Submit feedback to backend
   */
  submitFeedback(): void {
    if (this.selectedHarvest && this.selectedHarvest.status === 'ready') {
      console.log('Submitting feedback:', {
        harvestId: this.selectedHarvest.id,
        answers: this.userAnswers
      });

      const answersToSend = this.userAnswers.map(ans => {
        const question = this.feedbackQuestions.find(q => q.id === ans.questionId);

        return {
          questionId: ans.questionId,
          targetParameter: question?.targetParameter,
          selectedOption: {
            label: ans.selectedOption.label,
            value: ans.selectedOption.value,
            multiplier: ans.selectedOption.multiplier
          }
        };
      });

      const cropName = this.selectedHarvest.cropType;

      this.farmService.submitFeedback(this.selectedHarvest.id, answersToSend).subscribe({
        next: () => {
          this.selectedHarvest!.status = 'completed';
          this.selectedHarvest!.feedback = {
            submittedAt: new Date(),
            answers: this.userAnswers
          };

          // Close modal first
          this.closeFeedbackModal();

          // Show success toast
          this.toastr.success(
            `Your feedback for ${cropName} has been recorded`,
            'Feedback Submitted!'
          );
        },
        error: (err) => {
          console.error('Error submitting feedback', err);

          // Show error toast
          this.toastr.error(
            'Please try again or contact support',
            'Failed to Submit Feedback'
          );
        }
      });
    }
  }

  /**
   * Get questions and answers for viewing feedback
   */
  getQuestionsAndAnswers(): Array<{question: string, answer: string}> {
    if (!this.selectedHarvest?.feedback?.answers) {
      return [];
    }

    return this.selectedHarvest.feedback.answers.map(answer => {
      const question = this.feedbackQuestions.find(q => q.id === answer.questionId);
      return {
        question: question?.question || 'Unknown question',
        answer: answer.selectedOption.label
      };
    });
  }
  restartFeedback(): void {
    if (this.selectedHarvest && this.selectedHarvest.status === 'completed') {
      const cropName = this.selectedHarvest.cropType;
      const harvestId = this.selectedHarvest.id;

      console.log('Attempting to restart feedback for harvest:', harvestId);

      // Show confirmation
      if (!confirm(`Are you sure you want to restart feedback for ${cropName}? Your previous feedback will be deleted.`)) {
        console.log('User cancelled restart');
        return;
      }

      this.farmService.deleteFeedback(harvestId).subscribe({
        next: () => {
          console.log('Feedback deleted successfully');

          // Find and update the harvest in the list
          const harvestIndex = this.harvests.findIndex(h => h.id === harvestId);
          if (harvestIndex !== -1) {
            this.harvests[harvestIndex].status = 'ready';
            this.harvests[harvestIndex].feedback = undefined;
          }

          // Close the view modal
          this.showViewFeedbackModal = false;

          // Small delay to ensure UI updates before opening new modal
          setTimeout(() => {
            const updatedHarvest = this.harvests.find(h => h.id === harvestId);
            if (updatedHarvest) {
              this.openFeedbackModal(updatedHarvest);
            }
          }, 100);

          // Show success toast
          this.toastr.success(
            `You can now submit new feedback for ${cropName}`,
            'Feedback Restarted!'
          );
        },
        error: (err) => {
          console.error('Error deleting feedback:', err);
          console.error('Error details:', {
            status: err.status,
            message: err.message,
            error: err.error
          });

          this.toastr.error(
            err.error?.message || 'Please try again or contact support',
            'Failed to Delete Feedback'
          );
        }
      });
    } else {
      console.error('Cannot restart feedback - invalid harvest state:', this.selectedHarvest);
      this.toastr.warning('This harvest feedback cannot be restarted', 'Invalid State');
    }
  }

  showResetConfirmModal = false;

  openResetConfirmModal(): void {
    this.showResetConfirmModal = true;
  }

  closeResetConfirmModal(): void {
    this.showResetConfirmModal = false;
  }

  confirmResetAllFeedback(): void {
    this.farmService.selectedFarm$.pipe(take(1)).subscribe(farm => {
      if (!farm || !farm.id) {
        this.toastr.warning('No farm selected', 'Cannot Reset');
        return;
      }

      const farmName = farm.name;
      const farmId = farm.id;

      console.log('Resetting all feedback for farm:', farmId);

      this.farmService.deleteAllFeedbackForFarm(farmId).subscribe({
        next: () => {
          console.log('All feedback deleted successfully');

          // Close the modal
          this.closeResetConfirmModal();

          // Remove all harvest history items (keep only locked fields)
          this.harvests = this.harvests.filter(h => h.status === 'locked');

          // Show success toast
          this.toastr.success(
            `All harvest history and feedback deleted. Rule-based engine reset to default.`,
            'Reset Complete!'
          );
        },
        error: (err) => {
          console.error('Error deleting all feedback:', err);
          console.error('Error details:', {
            status: err.status,
            message: err.message,
            error: err.error
          });

          // Close modal
          this.closeResetConfirmModal();

          this.toastr.error(
            err.error?.message || 'Please try again or contact support',
            'Failed to Reset Feedback'
          );
        }
      });
    });
  }
}
