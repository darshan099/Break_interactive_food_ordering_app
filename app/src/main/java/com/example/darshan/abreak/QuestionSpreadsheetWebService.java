package com.example.darshan.abreak;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface QuestionSpreadsheetWebService {
    @POST("1FAIpQLSfKsI0Lw8cHZeirK21X9GcwO1kR8wdxwD-4iHar2y4n21P8Ug/formResponse")
    @FormUrlEncoded
    Call<Void> coompleteQuestionnaire(
            @Field("entry.572561570") String time,
            @Field("entry.1665478766") String name,
            @Field("entry.937300265") String dosa,
            @Field("entry.1559451452") String idly,
            @Field("entry.1941652881") String pongal,
            @Field("entry.54544917") String burger,
            @Field("entry.436605953") String pizza,
            @Field("entry.1705380282") String sandwich,
            @Field("entry.846275926") String count
    );
}