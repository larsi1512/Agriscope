import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

import {
  Chart,
  BarController,
  BarElement,
  DoughnutController,
  PieController,
  CategoryScale,
  LinearScale,
  ArcElement,
  Tooltip,
  Legend
} from 'chart.js';

Chart.register(
  BarController,
  BarElement,
  DoughnutController,
  PieController,
  CategoryScale,
  LinearScale,
  ArcElement,
  Tooltip,
  Legend
);

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));



