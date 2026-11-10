package com.miniproject.marbleshooter.game

// Source-compatibility aliases keep older callers stable while implementation
// lives in dedicated domain and model packages.
typealias GameEngine = com.miniproject.marbleshooter.game.domain.GameEngine
typealias Vec2 = com.miniproject.marbleshooter.game.domain.model.Vec2
typealias Ball = com.miniproject.marbleshooter.game.domain.model.Ball
typealias Projectile = com.miniproject.marbleshooter.game.domain.model.Projectile
typealias GameStatus = com.miniproject.marbleshooter.game.domain.model.GameStatus
typealias GameSaveState = com.miniproject.marbleshooter.game.domain.model.GameSaveState
typealias GameEvent = com.miniproject.marbleshooter.game.domain.model.GameEvent
typealias BoosterType = com.miniproject.marbleshooter.game.domain.model.BoosterType
typealias BoosterInventory = com.miniproject.marbleshooter.game.domain.model.BoosterInventory
