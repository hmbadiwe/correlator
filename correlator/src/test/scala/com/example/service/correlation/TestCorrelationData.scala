package com.example.service.correlation



trait TestCorrelationData {

    val testMessagesWithoutCorrelationIds = List(
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_1|1",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "firstOccurrence" -> "1376156096932"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_2|2",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "firstOccurrence" -> "1376156096932"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_3|3",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "firstOccurrence" -> "1376156096932"
      )
    )
  val testMessagesWithSameCorrelationIds = List(
    CommonMessage(
      "alarmIdentifier" -> "test_agent_id_4|4",
      "correlationLevel" -> "device_id",
      "device_id" -> "test_device_1",
      "correlatedId" -> "test_agent_id_1|1",
      "firstOccurrence" -> "1376156096932"
    ),
    CommonMessage(
      "alarmIdentifier" -> "test_agent_id_5|5",
      "correlationLevel" -> "device_id",
      "device_id" -> "test_device_1",
      "correlatedId" -> "test_agent_id_1|1",
      "firstOccurrence" -> "1376156096932"
    ),
    CommonMessage(
      "alarmIdentifier" -> "test_agent_id_6|6",
      "correlationLevel" -> "device_id",
      "device_id" -> "test_device_1",
      "correlatedId" -> "test_agent_id_1|1",
      "firstOccurrence" -> "1376156096932"
    )
  )

  val testTimeAlarms = Map(
     "first_time" -> CommonMessage(
        "messageId" -> "1",
        "lastOccurrence" -> "time1",
        "device_id" -> "test_device_1",
        "correlationLevel" -> "lastOccurrence:-5m/device_id"
     ),
      "first_time_plus_30_secs" -> CommonMessage(
        "messageId" -> "2",
        "lastOccurrence" -> "time1_plus_30",
        "device_id" -> "test_device_1",
        "correlationLevel" -> "lastOccurrence:-30s/device_id"
      ),
      "first_time_plus_3_mins" -> CommonMessage(
        "messageId" -> "3",
        "lastOccurrence" -> "time1_plus_3_mins",
        "device_id" -> "test_device_1",
        "correlationLevel" -> "lastOccurrence:-5m/device_id"
      )
    )

    val testAlarms = Map(
      "first" -> CommonMessage(
        Map(
           "messageId" -> "1",
           "alarmIdentifier" -> "test_agent_id_1|1",
           "correlationLevel" -> "device_id",
           "device_id" -> "test_device_1"
        )
      ),
      "second_non_correlated" -> CommonMessage(
        Map(
          "messageId" -> "2",
          "alarmIdentifier" -> "test_agent_id_2|2",
           "correlationLevel" -> "alarm_id",
           "device_id" -> "test_device_2c",
           "alarm_id" ->  "test_alarm_2c"
        )
      ),
      "second_correlated" -> CommonMessage(
        Map(
          "messageId" -> "3",
          "alarmIdentifier" -> "test_agent_id_3|3",
           "correlationLevel" -> "device_id",
           "device_id" -> "test_device_1"
        )
      ),
      "third_correlated" -> CommonMessage(
        Map(
          "messageId" -> "4",
          "alarmIdentifier" -> "test_agent_id_3_1|3_1",
          "correlationLevel" -> "device_id",
          "device_id" -> "test_device_1"
        )
      ),
      "first_multi_correlated" -> CommonMessage(
        Map(
          "messageId" -> "4",
          "alarmIdentifier" -> "test_agent_id_4|4",
          "correlationLevel" -> "device_id/alarm_id",
          "alarm_id" -> "test_alarm_1",
          "device_id" -> "test_device_1"
        )
      ),
      "second_multi_correlated" -> CommonMessage(
        Map(
          "messageId" -> "6",
          "alarmIdentifier" -> "test_agent_id_6|6",
          "correlationLevel" -> "device_id/alarm_id",
          "alarm_id" -> "test_alarm_1",
          "device_id" -> "test_device_1"
        )
      ),
      "third_multi_correlated" -> CommonMessage(
        Map(
          "messageId" -> "16",
          "alarmIdentifier" -> "test_agent_id_16|16",
          "correlationLevel" -> "device_id/alarm_id",
          "alarm_id" -> "test_alarm_1",
          "device_id" -> "test_device_1"
        )
      ),
      "fourth_multi_correlated" -> CommonMessage(
        Map(
          "messageId" -> "17",
          "alarmIdentifier" -> "test_agent_id_17|17",
          "correlationLevel" -> "device_id/alarm_id",
          "alarm_id" -> "test_alarm_1",
          "device_id" -> "test_device_1"
        )
      ),
      "fifth_multi_correlated" -> CommonMessage(
        Map(
          "messageId" -> "18",
          "alarmIdentifier" -> "test_agent_id_18|18",
          "correlationLevel" -> "device_id/alarm_id",
          "alarm_id" -> "test_alarm_1",
          "device_id" -> "test_device_1"
        )
      ),
      "second_multi_non_correlated" -> CommonMessage(
        Map(
          "messageId" -> "7",
          "alarmIdentifier" -> "test_agent_id_7|7",
          "correlationLevel" -> "device_id/alarm_id",
          "device_id" -> "test_device_1",
          "alarm_id" -> "test_alarm_x"
        )
      ),
      "full_multi_correlated" -> CommonMessage(
        Map(
          "messageId" -> "8",
          "correlationLevel" -> "device_id/alarm_id/ip_address",
          "alarmIdentifier" -> "test_agent_id_8|8",
          "device_id" -> "test_device_id",
          "alarm_id" -> "test_alarm_id",
          "ip_address" -> "10.0.0.1",
          "port" -> "8590",
          "locale" -> "EN_US"

        )
      ),
      "second_no_correlatable_fields" -> CommonMessage(
        "messageId" -> "9",
        "alarmIdentifier" -> "test_agent_id_9|9"
      ),
      "third_no_correlatable_values" -> CommonMessage(
        "messageId" -> "10",
        "alarmIdentifier" -> "test_agent_id_10|10",
        "correlationLevel" -> "device"
      )

    )
    val testMessagesWithDifferentCorrelationIds = List(
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_6|6",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_4|4",
        "firstOccurrence" -> "1376156096932"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_7|7",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_3|3",
        "firstOccurrence" -> "1376156096922"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_8|8",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_2|2",
        "firstOccurrence" -> "1376156096942"
      )
    )

    val testMessagesWithDifferentCorrelationIdsWithRoot = List(
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_6|6",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_4|4",
        "firstOccurrence" -> "1376156096932"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_7|7",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_3|3",
        "firstOccurrence" -> "1376156096922"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_8|8",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_2|2",
        "firstOccurrence" -> "1376156096942"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_8|8",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_8|8",
        "firstOccurrence" -> "1376156096972"
      )
    )

    val testMessagesWithDifferentCorrelationIdsWithMultipleRoots = List(
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_11|11",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_11|11",
        "firstOccurrence" -> "1376156096932"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_12|12",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_12|12",
        "firstOccurrence" -> "1376156096912"
      ),
      CommonMessage(
        "alarmIdentifier" -> "test_agent_id_13|13",
        "correlationLevel" -> "device_id",
        "device_id" -> "test_device_1",
        "correlatedId" -> "test_agent_id_13|13",
        "firstOccurrence" -> "1376156096922"
      )

    )

    val testMessageWithTimeCorrelation = List(
      CommonMessage(
        "correlationLevel" -> "firstOccurrence<>(-100,0)",
        "firstOccurrence" -> "1378849736",
        "alarmIdentifier" -> "test_alarm_0"
      ),
      CommonMessage(
        "correlationLevel" -> "firstOccurrence<>(-100,0)",
        "firstOccurrence" -> "1378849786",
        "alarmIdentifier" -> "test_alarm_1"
      )
      ,CommonMessage(
        "correlationLevel" -> "firstOccurrence<>(-100,0)",
        "firstOccurrence" -> "1378849835",
        "alarmIdentifier" -> "test_alarm_2"
        )
      ,CommonMessage(
        "correlationLevel" -> "firstOccurrence<>(-100,0)",
        "firstOccurrence" -> "1378849936",
        "alarmIdentifier" -> "test_alarm_3"
      )
    )
}
