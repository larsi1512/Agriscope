import { Component, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { TopbarComponent } from '../topbar/topbar';
import { Sidebar } from '../sidebar/sidebar';
import { FarmService } from '../../services/farm-service/farm-service';
import { AnalyticsService } from '../../services/analytics-service/analytics-service';

@Component({
  selector: 'app-statistic',
  standalone: true,
  imports: [CommonModule, TopbarComponent, Sidebar, BaseChartDirective],
  templateUrl: './statistic.html',
  styleUrls: ['./statistic.css']
})
export class StatisticComponent implements OnInit {
  currentFarmName: string = '';
  currentFarmId: string | null = null;

  loadingFeedback = true;
  loadingDashboard = true;

  private readonly ALERT_COLORS: string[] = [
    '#4CAF50', // green
    '#FF9800', // orange
    '#2196F3', // blue
    '#9C27B0', // purple
    '#FFC107', // amber
    '#009688', // teal
    '#E91E63', // pink
    '#795548', // brown
    '#607D8B', // blue-grey
    '#8BC34A', // light green
    '#3F51B5'  // indigo
  ];

  private readonly CROP_COLORS: string[] = [
    '#8BC34A', // light green
    '#4CAF50', // green
    '#FFB74D', // light orange
    '#FF9800', // orange
    '#A1887F', // brown
    '#81C784'  // soft green
  ];


  // Feedback Chart (Chart 1)
  feedbackChartData: ChartConfiguration['data'] = {
    datasets: [{
      data: [],
      label: 'Feedback Count',
      backgroundColor: 'rgba(139, 69, 19, 0.6)',
      borderColor: 'rgba(139, 69, 19, 1)',
      borderWidth: 2,
    }],
    labels: []
  };
  feedbackChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      },
      title: {
        display: true,
        text: 'Prediction Accuracy (User Feedback)',
        color: '#5d4037',
        font: {
          size: 18,
          weight: "normal"
        },
        padding: {
          top: 10,
          bottom: 20
        }
      },
      tooltip: {
        callbacks: {
          label: (ctx) => `Feedback count: ${ctx.raw}`
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(0,0,0,0.08)'
        },
        title: {
          display: true,
          text: 'Number of Feedback Entries'
        }
      },
      x: {
        grid: {
          display: false
        },
        title: {
          display: true,
          text: 'Prediction Quality'
        }
      }
    }
  };
  feedbackChartType: ChartType = 'bar';

  // Alert Distribution Chart (Chart 2)
  alertChartData: ChartConfiguration['data'] = {
    datasets: [{
      data: [],
      label: 'Alert Count',
      backgroundColor: [
        'rgba(255, 99, 132, 0.6)',
        'rgba(54, 162, 235, 0.6)',
        'rgba(255, 206, 86, 0.6)',
        'rgba(75, 192, 192, 0.6)',
        'rgba(153, 102, 255, 0.6)',
      ],
      borderColor: [
        'rgba(255, 99, 132, 1)',
        'rgba(54, 162, 235, 1)',
        'rgba(255, 206, 86, 1)',
        'rgba(75, 192, 192, 1)',
        'rgba(153, 102, 255, 1)',
      ],
      borderWidth: 2,
    }],
    labels: []
  };
  alertChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false
      },
      title: {
        display: true,
        text: 'System Alerts by Severity',
        color: '#5d4037',
        font: {
          size: 18,
          weight: 600
        },
        padding: {
          top: 10,
          bottom: 20
        }
      },
      tooltip: {
        callbacks: {
          label: (ctx) => `Alerts triggered: ${ctx.raw}`
        }
      }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(0,0,0,0.08)'
        },
        title: {
          display: true,
          text: 'Number of Alerts'
        }
      },
      x: {
        grid: {
          display: false
        }
      }
    }
  };
  alertChartType: ChartType = 'bar';

  // Crop Vulnerability Chart (Chart 3)
  cropChartData: ChartConfiguration['data'] = {
    datasets: [{
      data: [],
      label: 'Alerts per Crop',
      backgroundColor: [
        'rgba(255, 159, 64, 0.6)',
        'rgba(255, 205, 86, 0.6)',
        'rgba(201, 203, 207, 0.6)',
        'rgba(139, 195, 74, 0.6)',
      ],
      borderColor: [
        'rgba(255, 159, 64, 1)',
        'rgba(255, 205, 86, 1)',
        'rgba(201, 203, 207, 1)',
        'rgba(139, 195, 74, 1)',
      ],
      borderWidth: 2,
    }],
    labels: []
  };
  cropChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'right',
        labels: {
          boxWidth: 14,
          padding: 16,
          font: {
            size: 13
          }
        }
      },
      title: {
        display: true,
        text: 'Crop Vulnerability (Alerts by Crop)',
        color: '#5d4037',
        font: {
          size: 18,
          weight: 600
        },
        padding: {
          top: 10,
          bottom: 20
        }
      },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.label}: ${ctx.raw} alerts`
        }
      }
    }
  };

  cropChartType: ChartType = 'doughnut';

  // Water Savings Chart (Chart 4)
  waterChartData: ChartConfiguration['data'] = {
    datasets: [{
      data: [],
      label: 'Irrigation Actions',
      backgroundColor: ['rgba(33, 150, 243, 0.6)', 'rgba(76, 175, 80, 0.6)'],
      borderColor: ['rgba(33, 150, 243, 1)', 'rgba(76, 175, 80, 1)'],
      borderWidth: 2,
    }],
    labels: ['Actions Taken', 'Actions Saved']
  };
  waterChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: true,
        position: 'bottom',
        labels: {
          boxWidth: 14,
          padding: 18,
          font: {
            size: 13
          }
        }
      },
      title: {
        display: false
      },
      tooltip: {
        callbacks: {
          label: (ctx) => `${ctx.label}: ${ctx.raw}`
        }
      }
    }
  };
  waterChartType: ChartType = 'pie';

  constructor(
    private farmService: FarmService,
    private analyticsService: AnalyticsService
  ) {}

  ngOnInit(): void {
    this.farmService.selectedFarm$.subscribe(farm => {
      if (farm && farm.id) {
        this.currentFarmName = farm.name;
        this.currentFarmId = farm.id;
        this.loadAllAnalytics(farm.id);
      } else {
        this.currentFarmName = '';
        this.currentFarmId = null;
      }
    });
  }

  loadAllAnalytics(farmId: string): void {
    this.loadFeedbackStats(farmId);
    this.loadDashboardAnalytics(farmId);
  }

  loadFeedbackStats(farmId: string): void {
    this.loadingFeedback = true;
    this.analyticsService.getFeedbackStats(farmId).subscribe({
      next: (data) => {
        console.log('Feedback stats:', data);
          const labels = ['-2 (Late/Too Dry)', '-1 (Slightly late / Somewhat off)', '0 (Accurate)', '1 (Somewhat early)', '2 (Too early / False alarm)'];
        const values = [
          data['-2'] || 0,
          data['-1'] || 0,
          data['0'] || 0,
          data['1'] || 0,
          data['2'] || 0
        ];

        this.feedbackChartData = {
          labels: [
            '-2 (Late / Too Dry)',
            '-1 (Slightly late / Somewhat off)',
            '0 (Accurate)',
            '1 (Somewhat early)',
            '2 (Too early / False alarm)'
          ],
          datasets: [
            {
              data: values,
              label: 'Feedback Count',
              backgroundColor: [
                'rgba(211, 47, 47, 0.7)',   // red
                'rgba(255, 193, 7, 0.7)',  // yellow
                'rgba(76, 175, 80, 0.8)',  // green
                'rgba(255, 193, 7, 0.7)',  // yellow
                'rgba(211, 47, 47, 0.7)',   // red
              ],
              borderColor: [
                '#c62828',
                '#f9a825',
                '#388e3c',
                '#f9a825',
                '#c62828',
              ],
              borderWidth: 2,
              borderRadius: 8
            }
          ]
        };
        this.loadingFeedback = false;
      },
      error: (err) => {
        console.error('Error loading feedback stats:', err);
        this.loadingFeedback = false;
      }
    });
  }

  loadDashboardAnalytics(farmId: string): void {
    this.loadingDashboard = true;
    this.analyticsService.getDashboardAnalytics(farmId).subscribe({
      next: (data) => {
        console.log('Dashboard analytics:', data);

        // Alert Distribution (Chart 2)
        const alertLabels = Object.keys(data.alertDistribution).map(key =>
          this.formatAlertType(key)
        );
        const alertValues = Object.values(data.alertDistribution);

        this.alertChartData = {
          labels: alertLabels,
          datasets: [
            {
              data: alertValues,
              label: 'Alert Count',
              backgroundColor: this.ALERT_COLORS
                .slice(0, alertLabels.length)
                .map(c => c + 'CC'),

              borderColor: this.ALERT_COLORS
                .slice(0, alertLabels.length),

              borderWidth: 2,
              borderRadius: 8
            }
          ]
        };

        // Crop Vulnerability (Chart 3)
        const cropLabels = Object.keys(data.cropVulnerability).map(key =>
          this.formatCropName(key)
        );
        const cropValues = Object.values(data.cropVulnerability);

        this.cropChartData = {
          labels: cropLabels,
          datasets: [
            {
              data: cropValues,
              label: 'Alerts per Crop',
              backgroundColor: cropLabels.map(
                (_, i) => this.CROP_COLORS[i % this.CROP_COLORS.length] + 'CC'
              ),
              borderColor: cropLabels.map(
                (_, i) => this.CROP_COLORS[i % this.CROP_COLORS.length]
              ),
              borderWidth: 2
            }
          ]
        };

        // Water Savings (Chart 4)
        this.waterChartData = {
          labels: ['Irrigation Performed', 'Irrigation Saved'],
          datasets: [
            {
              data: [
                data.waterSavings.actionsTaken,
                data.waterSavings.actionsSaved
              ],
              backgroundColor: [
                'rgba(100, 181, 246, 0.7)', // soft blue
                'rgba(129, 199, 132, 0.8)'  // soft green
              ],
              borderColor: [
                '#1e88e5',
                '#388e3c'
              ],
              borderWidth: 2
            }
          ]
        };

        this.loadingDashboard = false;
      },
      error: (err) => {
        console.error('Error loading dashboard analytics:', err);
        this.loadingDashboard = false;
      }
    });
  }

  formatAlertType(alertType: string): string {
    return alertType
      .split('_')
      .map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ');
  }

  formatCropName(cropName: string): string {
    return cropName.charAt(0).toUpperCase() + cropName.slice(1).toLowerCase();
  }

  hasFeedbackData(): boolean {
    const data = this.feedbackChartData.datasets[0]?.data as number[] | undefined;
    return !!data && data.some(v => v > 0);
  }

  hasAlertData(): boolean {
    const data = this.alertChartData.datasets[0]?.data as number[] | undefined;
    return !!data && data.some(v => v > 0);
  }

  hasCropData(): boolean {
    const data = this.cropChartData.datasets[0]?.data as number[] | undefined;
    return !!data && data.some(v => v > 0);
  }

  hasWaterData(): boolean {
    const data = this.waterChartData.datasets[0]?.data as number[] | undefined;
    return !!data && data.reduce((a, b) => a + b, 0) > 0;
  }

  getWaterSavedPercentage(): number {
    const data = this.waterChartData.datasets[0]?.data as number[] | undefined;

    if (!data || data.length !== 2) return 0;

    const taken = data[0];
    const saved = data[1];
    const total = taken + saved;

    return total > 0 ? Math.round((saved / total) * 100) : 0;
  }
}
