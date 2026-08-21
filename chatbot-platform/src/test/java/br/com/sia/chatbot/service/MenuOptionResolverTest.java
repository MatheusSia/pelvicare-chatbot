package br.com.sia.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuOptionResolverTest {

    private MenuOptionResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new MenuOptionResolver();
    }

    @ParameterizedTest
    @CsvSource({
            "'1', '1'",
            "'Quais tratamentos vocês oferecem?', '1'",

            "'2', '2'",
            "'Vocês aceitam Unimed?', '2'",

            "'3', '3'",
            "'Qual o código da guia Unimed?', '3'",
            "'Preciso da guia de tratamento', '3'",

            "'4', '4'",
            "'Qual o horário de funcionamento?', '4'",

            "'5', '5'",
            "'Qual é o endereço?', '5'",
            "'Tem estacionamento com manobrista?', '5'",

            "'6', '6'",
            "'Vocês atendem homens?', '1'",
            "'Atendem crianças?', '1'",

            "'7', '7'",
            "'Quero falar com uma pessoa', '6'",
            "'Qual é o valor da consulta?', '6'",

            "'9', 'unknown'",
            "'Menu', 'menu'",
            "'Bom dia', 'unknown'"
    })
    void shouldIdentifyMenuOption(
            String input,
            String expected
    ) {
        assertEquals(
                expected,
                resolver.identify(input)
        );
    }
}