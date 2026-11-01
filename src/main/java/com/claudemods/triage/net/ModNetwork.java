package com.claudemods.triage.net;

import com.claudemods.triage.TriageMod;
import com.claudemods.triage.client.ClientBody;
import com.claudemods.triage.client.ClientTags;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TriageMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // The client class is only touched inside the lambda, so a dedicated server
        // never loads it.
        registrar.playToClient(SyncBodyPayload.TYPE, SyncBodyPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.accept(payload));
        registrar.playToClient(OpenTreatmentPayload.TYPE, OpenTreatmentPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.openTreatment(payload));
        registrar.playToClient(CloseTreatmentPayload.TYPE, CloseTreatmentPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.interruptTreatment());
        registrar.playToClient(UpdatePatientPayload.TYPE, UpdatePatientPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.updatePatient(payload));
        registrar.playToClient(ShowBodyPayload.TYPE, ShowBodyPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.showBody(payload));
        registrar.playToClient(ShowFilmPayload.TYPE, ShowFilmPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.showFilm(payload));
        registrar.playToClient(SyncTagPayload.TYPE, SyncTagPayload.STREAM_CODEC,
                (payload, context) -> ClientTags.accept(payload));
        registrar.playToClient(OpenAidPayload.TYPE, OpenAidPayload.STREAM_CODEC,
                (payload, context) -> ClientBody.openAid(payload));

        registrar.playToServer(TreatmentDonePayload.TYPE, TreatmentDonePayload.STREAM_CODEC,
                TreatmentDonePayload::handle);
        registrar.playToServer(GiveUpPayload.TYPE, GiveUpPayload.STREAM_CODEC, GiveUpPayload::handle);
        registrar.playToServer(PutDownPayload.TYPE, PutDownPayload.STREAM_CODEC, PutDownPayload::handle);
        registrar.playToServer(WriteTagPayload.TYPE, WriteTagPayload.STREAM_CODEC, WriteTagPayload::handle);
        registrar.playToServer(AidPayload.TYPE, AidPayload.STREAM_CODEC, AidPayload::handle);
        registrar.playToServer(AidDonePayload.TYPE, AidDonePayload.STREAM_CODEC, AidDonePayload::handle);
    }
}
