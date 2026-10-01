package br.com.sia.chatbot.config;

import br.com.sia.chatbot.entity.ChatbotMessage;
import br.com.sia.chatbot.flow.MessageCode;
import br.com.sia.chatbot.repository.ChatbotMessageRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ChatbotDataInitializer
        implements CommandLineRunner {

    private static final String INSTANCE = "pelvicare";

    private final ChatbotMessageRepository repository;

    public ChatbotDataInitializer(
            ChatbotMessageRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        upsert(
                MessageCode.LGPD,
                "LGPD",
                """
                🔒 *Privacidade e proteção de dados*

                Para sua segurança e em conformidade com a LGPD, utilizaremos este WhatsApp para comunicações relacionadas ao seu atendimento, como orientações, informações da clínica e futuros agendamentos.

                Seus dados serão utilizados exclusivamente para o atendimento da clínica.

                Responda com o número da opção:

                *1* - Continuar com o atendimento automático
                *2* - Já estou falando com a clínica (encerrar o bot)
                """
        );

        upsert(
                MessageCode.LGPD_RETRY,
                "Repetição do aceite da LGPD",
                """
                Para continuarmos, responda:

                *1* - Continuar com o atendimento automático
                *2* - Já estou falando com a clínica (encerrar o bot)
                """
        );

        upsert(
                MessageCode.BOT_STOPPED,
                "Atendimento automático encerrado",
                """
                Tudo bem. O atendimento automático foi encerrado nesta conversa.

                Nossa equipe continua disponível por aqui.
                """
        );

        createIfNotExists(
                MessageCode.MAIN_MENU,
                "Menu principal",
                """
                *Bem-vindo(a) à Clínica PelviCare!*
        
                Somos especializados em:
        
                • Fisioterapia Pélvica
                • Fisioterapia Obstétrica
                • Fisioterapia em Oncologia Feminina
        
                Como podemos ajudar?
        
                *1* - Tratamentos oferecidos
                *2* - Planos de saúde atendidos
                *3* - Guia de tratamento pelo Plano
                *4* - Horário de atendimento
                *5* - Endereço
                *6* - Falar com a secretária
                *7* - Fisioterapia para outras áreas
        
                Digite o número da opção desejada.
                """
        );

        createIfNotExists(
                MessageCode.TREATMENTS,
                "Tratamentos oferecidos",
                """
                *Tratamentos oferecidos*

                • Fisioterapia Pélvica para mulheres, homens e crianças
                • Fisioterapia Obstétrica
                • Reabilitação abdominal pós-parto
                • Fisioterapia pós-operatória
                • Taping pós-parto
                • Laserterapia
                • Fisioterapia em Oncologia Feminina

                Digite *menu* para visualizar as opções novamente.
                """
        );

        createIfNotExists(
                MessageCode.HEALTH_PLANS,
                "Planos de saúde atendidos",
                """
                *Planos de saúde atendidos para fisioterapia pélvica*

                • Unimed
                • NotreDame Intermédica / Hapvida
                • Amil One
                • Petrobras
                • SPA Saúde
                • MedSênior
                • Care Plus
                
                Verifique com a secretária se o seu tipo plano é atendido por nós.

                As demais modalidades de atendimento são particulares.

                Digite *menu* para voltar.
                """
        );

        createIfNotExists(
                MessageCode.SCHEDULE,
                "Horários",
                """
                *Horários*

                Secretária:
                Segunda a sexta, das 08:00 às 17:00.

                Atendimentos de fisioterapia:
                Segunda a sexta, das 07:30 às 19:00.

                Digite *menu* para voltar.
                """
        );

        createIfNotExists(
                MessageCode.ADDRESS,
                "Endereço",
                """
                *Endereço*
        
                Avenida Orosimbo Maia, 430, Edificio Easy Office — Sala 809 — 8º andar — Campinas/SP
                
                📍 Abrir no Google Maps:
                https://www.google.com/maps/search/?api=1&query=Avenida+Orosimbo+Maia+430+Easy+Office+Campinas+SP
        
                *Estacionamento pago com manobrista*
        
                Há estacionamento com manobrista na porta do prédio. O valor é pago separadamente pelo paciente.
        
                Horário do manobrista:
                Até as *19h*.
        
                *Estacionamento pago sem manobrista*
        
                Também é possível estacionar no prédio pela Rua Barata Ribeiro, 79
                
                📍 Abrir no Google Maps:
                https://www.google.com/maps/search/?api=1&query=Rua+Barata+Ribeiro+79+Campinas+SP
        
                O estacionamento sem manobrista funciona até as *20h*.
        
                Digite *menu* para voltar.
                """
        );

        createIfNotExists(
                MessageCode.UNIMED_GUIDE,
                "Guia de tratamento pelo Plano",
                """
                *Guia de tratamento pelo Plano*

                Para atendimentos pelo plano, precisamos de uma guia válida e autorizada com o código:

                *20103646 — Reabilitação Perineal com Biofeedback*
                ou
                *50000233 — Atendimento fisioterapêutico do aparelho gênito-urinário*

                Preferencialmente, a guia deve conter autorização para *10 sessões*.

                Você já possui essa guia?

                Para fazer seu agendamento com a secretária, digite *6*.
                
                Digite *menu* para voltar.
                """
        );

        createIfNotExists(
                MessageCode.INVALID_OPTION,
                "Opção inválida",
                """
                Não consegui identificar a opção.
        
                Digite um número de *1 a 7* ou escreva *menu* para visualizar as opções.
                """
        );

        createIfNotExists(
                MessageCode.REQUEST_NAME,
                "Solicitação de nome",
                """
                Para encaminharmos seu atendimento à secretária, informe seu *nome completo*.
                """
        );

        createIfNotExists(
                MessageCode.HANDOFF_CONFIRMATION,
                "Confirmação de encaminhamento",
                """
                Obrigado, *{{name}}*!
        
                Seu atendimento foi encaminhado para nossa secretária.
        
                *Assunto informado:*
                {{reason}}
        
                Nosso horário de atendimento é de segunda a sexta, das 08:00 às 17:00.
        
                Em breve responderemos por aqui.
                """
        );

        createIfNotExists(
                MessageCode.HANDOFF_EXPIRED,
                "Atendimento reiniciado",
                """
                Olá! Como o atendimento anterior foi encerrado, o atendimento automático foi iniciado novamente.

                {{mainMenu}}
                """
        );

        createIfNotExists(
                MessageCode.REQUEST_HANDOFF_REASON,
                "Solicitação do assunto",
                """
                Obrigado, *{{name}}*!
        
                Agora, conte brevemente qual é o assunto ou como podemos ajudar.
        
                Por exemplo:
        
                • Quero marcar uma avaliação
                • Tenho dúvida sobre meu convênio
                • Preciso alterar um horário
                """
        );

        createIfNotExists(
                MessageCode.INVALID_NAME,
                "Nome inválido",
                """
                Não consegui identificar seu nome.
        
                Por favor, informe seu *nome completo*.
                """
        );

        createIfNotExists(
                MessageCode.INVALID_HANDOFF_REASON,
                "Assunto inválido",
                """
                Por favor, descreva brevemente o motivo do contato para que a secretária possa ajudá-lo melhor.
                """
        );

        createIfNotExists(
                MessageCode.REFERRALS,
                "Fisioterapia para outras áreas",
                """
                *Fisioterapia para outras áreas*
        
                Para atendimentos de fisioterapia por planos relacionados a:
        
                • Joelho
                • Braço
                • Coluna
        
                Indicamos a:
                
                Clínica Pro Saúde
                *(19) 99391-8007*
                
                Campinas Fisioterapia
                *(19) 3231-5189*

                Digite *menu* para voltar às opções.
                """
        );

        createIfNotExists(
                MessageCode.UNSUPPORTED_AUDIO,
                "Áudio recebido",
                """
                Recebemos seu áudio. 🎧
        
                Neste momento, o atendimento automático ainda não consegue interpretar mensagens de voz.
        
                Por favor, escreva sua dúvida ou digite *6* para falar com a secretária.
                """
        );

        createIfNotExists(
                MessageCode.MEDIA_RECEIVED,
                "Arquivo recebido",
                """
                Recebemos o arquivo enviado.
        
                Para que nossa equipe possa verificar, o atendimento será encaminhado para a secretária.
        
                Por favor, informe seu *nome completo*.
                """
        );

        createIfNotExists(
                MessageCode.UNSUPPORTED_STICKER,
                "Figurinha recebida",
                """
                Não consegui interpretar a figurinha.
        
                Por favor, escreva como podemos ajudar ou digite *menu* para visualizar as opções.
                """
        );

        createIfNotExists(
                MessageCode.UNKNOWN_MESSAGE_TYPE,
                "Mensagem não reconhecida",
                """
                Não consegui interpretar esse tipo de mensagem.
        
                Por favor, envie sua dúvida em texto ou digite *6* para falar com a secretária.
                """
        );
    }

    private void createIfNotExists(
            String code,
            String title,
            String content
    ) {
        if (repository.existsByInstanceAndCode(
                INSTANCE,
                code
        )) {
            return;
        }

        ChatbotMessage message = new ChatbotMessage();
        message.setInstance(INSTANCE);
        message.setCode(code);
        message.setTitle(title);
        message.setContent(content);
        message.setActive(true);

        repository.save(message);
    }

    private void upsert(
            String code,
            String title,
            String content
    ) {
        repository.findByInstanceAndCode(
                INSTANCE,
                code
        ).ifPresentOrElse(
                existing -> {
                    existing.setTitle(title);
                    existing.setContent(content);
                    existing.setActive(true);
                    repository.save(existing);
                },
                () -> {
                    ChatbotMessage message = new ChatbotMessage();
                    message.setInstance(INSTANCE);
                    message.setCode(code);
                    message.setTitle(title);
                    message.setContent(content);
                    message.setActive(true);
                    repository.save(message);
                }
        );
    }
}