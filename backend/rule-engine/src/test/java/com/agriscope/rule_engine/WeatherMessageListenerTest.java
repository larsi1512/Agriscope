package com.agriscope.rule_engine;

import com.agriscope.rule_engine.domain.dto.FieldDTO;
import com.agriscope.rule_engine.domain.dto.WeatherForecastDTO;
import com.agriscope.rule_engine.domain.dto.WeatherMessageDTO;
import com.agriscope.rule_engine.domain.enums.SoilType;
import com.agriscope.rule_engine.domain.model.CurrentWeatherData;
import com.agriscope.rule_engine.domain.model.DailyWeatherData;
import com.agriscope.rule_engine.domain.model.FarmDetails;
import com.agriscope.rule_engine.domain.model.HourlyWeatherData;
import com.agriscope.rule_engine.messaging.WeatherMessageListener;
import com.agriscope.rule_engine.service.RuleEvaluationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WeatherMessageListenerTest {

    @Mock
    private RuleEvaluationService ruleEvaluationService;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private WeatherMessageListener listener;


    @Test
    void handleMessage_ShouldIgnoreInvalidMessage_NullFields() {
        WeatherMessageDTO invalidMsg = new WeatherMessageDTO();
        invalidMsg.setUserId("user1");

        listener.handleMessage(invalidMsg);

        verifyNoInteractions(ruleEvaluationService);
        verifyNoInteractions(restTemplate);
    }

    @Test
    void handleMessage_ShouldIgnoreMessage_WithEmptyForecastList() {
        WeatherMessageDTO msg = createBaseMessage("CURRENT");
        msg.setForecast(Collections.emptyList());

        listener.handleMessage(msg);

        verifyNoInteractions(ruleEvaluationService);
    }

    @Test
    void handleMessage_ShouldLogWarning_ForUnknownForecastType() {
        WeatherMessageDTO msg = createBaseMessage("UNKNOWN_TYPE");
        WeatherForecastDTO forecast = new WeatherForecastDTO();
        msg.setForecast(List.of(forecast));

        listener.handleMessage(msg);

        verifyNoInteractions(ruleEvaluationService);
        verifyNoInteractions(restTemplate);
    }

    //  CURRENT FORECAST

    @Test
    void handleMessage_ShouldProcessCurrentForecast_Success() {
        WeatherMessageDTO message = createBaseMessage("CURRENT");
        WeatherForecastDTO forecastDTO = new WeatherForecastDTO();
        forecastDTO.setTemperature2m(25.0);
        forecastDTO.setWindSpeed10m(5.5);
        forecastDTO.setTime("2023-10-01T10:00");
        message.setForecast(List.of(forecastDTO));

        mockRestTemplateResponse(Map.of("factor", 1.0));

        listener.handleMessage(message);

        ArgumentCaptor<CurrentWeatherData> captor = ArgumentCaptor.forClass(CurrentWeatherData.class);
        verify(ruleEvaluationService).evaluateCurrentDataForFarm(
                captor.capture(),
                anyList(),
                anyMap()
        );

        CurrentWeatherData data = captor.getValue();
        assertEquals(25.0, data.getTemperature_2m());
        assertNotNull(data.getTime());
    }

    @Test
    void handleMessage_ShouldAbortCurrent_IfForecastListIsEmptyInsideSwitch() {
        WeatherMessageDTO message = createBaseMessage("CURRENT");
        message.setForecast(Collections.emptyList());

        listener.handleMessage(message);

        verifyNoInteractions(ruleEvaluationService);
    }

    //  HOURLY FORECAST

    @Test
    void handleMessage_ShouldProcessHourlyForecast_WithSoilType() {
        WeatherMessageDTO message = createBaseMessage("HOURLY");
        message.setSoil_type("LOAM");

        WeatherForecastDTO forecastDTO = new WeatherForecastDTO();
        forecastDTO.setRain(2.0);
        forecastDTO.setTime("2023-10-01T12:00");
        message.setForecast(List.of(forecastDTO, forecastDTO));

        mockRestTemplateResponse(Map.of("factor", 1.2));

        listener.handleMessage(message);

        ArgumentCaptor<List<HourlyWeatherData>> listCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<FarmDetails> farmCaptor = ArgumentCaptor.forClass(FarmDetails.class);

        verify(ruleEvaluationService).evaluateHourlDataForFarm(
                listCaptor.capture(),
                farmCaptor.capture(),
                anyList()
        );

        List<HourlyWeatherData> capturedList = listCaptor.getValue();
        assertEquals(2, capturedList.size());
        assertEquals(2.0, capturedList.getFirst().getRain());

        FarmDetails capturedFarm = farmCaptor.getValue();
        assertEquals(SoilType.LOAM, capturedFarm.getSoilType());
    }

    //  DAILY FORECAST

    @Test
    void handleMessage_ShouldProcessDailyForecast_AndParseComplexDates() {
        WeatherMessageDTO message = createBaseMessage("DAILY");
        WeatherForecastDTO forecastDTO = new WeatherForecastDTO();

        forecastDTO.setTime("2023-10-01T12:00:00+02:00");
        message.setForecast(List.of(forecastDTO));

        mockRestTemplateResponse(Collections.emptyMap());

        listener.handleMessage(message);

        ArgumentCaptor<List<DailyWeatherData>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(ruleEvaluationService).evaluateDailyRules(listCaptor.capture(), anyList(), anyMap());

        DailyWeatherData data = listCaptor.getValue().getFirst();
        assertEquals(2023, data.getDate().getYear());
        assertEquals(12, data.getDate().getHour());
    }

    @Test
    void handleMessage_ShouldHandleMalformedDate_ByUsingCurrentTime() {
        WeatherMessageDTO message = createBaseMessage("DAILY");
        WeatherForecastDTO forecastDTO = new WeatherForecastDTO();
        forecastDTO.setTime("INVALID-DATE-FORMAT");
        message.setForecast(List.of(forecastDTO));

        mockRestTemplateResponse(Collections.emptyMap());

        listener.handleMessage(message);

        ArgumentCaptor<List<DailyWeatherData>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(ruleEvaluationService).evaluateDailyRules(listCaptor.capture(), anyList(), anyMap());

        DailyWeatherData data = listCaptor.getValue().get(0);
        assertNotNull(data.getDate());
        assertTrue(data.getDate().isAfter(LocalDateTime.now().minusSeconds(5)));
    }

    @Test
    void handleMessage_ShouldHandleNullDate_ByUsingCurrentTime() {
        WeatherMessageDTO message = createBaseMessage("DAILY");
        WeatherForecastDTO forecastDTO = new WeatherForecastDTO();
        forecastDTO.setTime(null);
        message.setForecast(List.of(forecastDTO));

        mockRestTemplateResponse(Collections.emptyMap());

        listener.handleMessage(message);

        ArgumentCaptor<List<DailyWeatherData>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(ruleEvaluationService).evaluateDailyRules(listCaptor.capture(), anyList(), anyMap());

        assertNotNull(listCaptor.getValue().get(0).getDate());
    }




    @Test
    void handleMessage_ShouldHandleFarmService_Exception() {
        WeatherMessageDTO message = createBaseMessage("CURRENT");
        message.setForecast(List.of(new WeatherForecastDTO()));

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenThrow(new RuntimeException("Service down"));

        listener.handleMessage(message);

        verify(ruleEvaluationService).evaluateCurrentDataForFarm(any(), any(), eq(Collections.emptyMap()));
    }

    @Test
    void handleMessage_ShouldHandleFarmService_NullBody() {
        WeatherMessageDTO message = createBaseMessage("CURRENT");
        message.setForecast(List.of(new WeatherForecastDTO()));

        ResponseEntity<Map<String, Double>> nullEntity = ResponseEntity.ok(null);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)))
                .thenReturn(nullEntity);

        listener.handleMessage(message);

        verify(ruleEvaluationService).evaluateCurrentDataForFarm(any(), any(), eq(Collections.emptyMap()));
    }

    @Test
    void handleMessage_GlobalException_ShouldLogAndNotThrow() {
        WeatherMessageDTO message = null;

        assertDoesNotThrow(() -> listener.handleMessage(message));
    }


    // HELPER METHODS

    private WeatherMessageDTO createBaseMessage(String type) {
        WeatherMessageDTO msg = new WeatherMessageDTO();
        msg.setUserId("user1");
        msg.setFarmId("farm1");
        msg.setEmail("test@test.com");
        msg.setSoil_type("LOAM");
        msg.setType(type);
        msg.setFields(List.of(new FieldDTO()));
        return msg;
    }

    private void mockRestTemplateResponse(Map<String, Double> body) {
        ResponseEntity<Map<String, Double>> responseEntity = ResponseEntity.ok(body);
        when(restTemplate.exchange(
                contains("farm-service"),
                eq(HttpMethod.GET),
                isNull(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);
    }
}